package xyz.stelchat.android;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded disk cache; no credentials, arbitrary URLs or renderer Base64 payloads. */
public final class EmojiDiskCache {
    public static final long LIMIT=50L*1024*1024, MAX_FILE=2L*1024*1024;
    public interface Downloader { byte[] load(String name) throws Exception; }
    private final File root;
    private final long limit;
    private final Downloader downloader;
    private final Semaphore slots=new Semaphore(2,true);
    private final ConcurrentHashMap<String,FutureTask<byte[]>> pending=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Long> failures=new ConcurrentHashMap<>();
    private final AtomicInteger epoch=new AtomicInteger();
    public EmojiDiskCache(File root){this(root,LIMIT,EmojiDiskCache::download);}
    public EmojiDiskCache(File root,long limit,Downloader downloader){this.root=root;this.limit=limit;this.downloader=downloader;}
    public static boolean valid(String name){return name!=null&&name.matches("[a-f0-9]{64}\\.(png|gif|webp|jpg|jpeg)");}
    private File file(String name){return new File(root,name);}
    public static String mime(byte[] b)throws IOException{
        if(b.length<12)throw new IOException("Short image");
        if(Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
        String head=new String(b,0,12,StandardCharsets.ISO_8859_1);
        if(head.startsWith("GIF87a")||head.startsWith("GIF89a"))return "image/gif";
        if((b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
        if(head.startsWith("RIFF")&&head.endsWith("WEBP"))return "image/webp";
        throw new IOException("Invalid image");
    }
    private static byte[] bounded(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[16384];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_FILE)throw new IOException("Image too large");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    private static byte[] download(String name)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL("https://stelchat.xyz/emoji-assets/"+name).openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
        try{
            if(c.getResponseCode()!=200||c.getContentLengthLong()>MAX_FILE)throw new IOException("Download rejected");
            try(InputStream in=c.getInputStream()){return bounded(in);}
        }finally{c.disconnect();}
    }
    private static byte[] hash(byte[] bytes)throws Exception{return MessageDigest.getInstance("SHA-256").digest(bytes);}
    private synchronized void remove(String name){file(name).delete();file(name+".sha256").delete();}
    private synchronized byte[] cached(String name){
        try{
            File f=file(name),sum=file(name+".sha256");
            if(f.length()<12||f.length()>MAX_FILE||sum.length()!=32)throw new IOException("Bad cache");
            byte[] bytes,checksum;
            try(InputStream in=new FileInputStream(f)){bytes=bounded(in);}
            try(InputStream in=new FileInputStream(sum)){checksum=bounded(in);}
            if(!MessageDigest.isEqual(hash(bytes),checksum))throw new IOException("Corrupt cache");
            mime(bytes);f.setLastModified(System.currentTimeMillis());return bytes;
        }catch(Exception e){remove(name);return null;}
    }
    public synchronized boolean has(String name){return valid(name)&&file(name).isFile()&&file(name).length()<=MAX_FILE;}
    public synchronized long[] stats(){long bytes=0,count=0;File[] files=root.listFiles();if(files!=null)for(File f:files)if(valid(f.getName())&&f.isFile()){bytes+=f.length();count++;}return new long[]{bytes,limit,count};}
    public synchronized void clear(){epoch.incrementAndGet();failures.clear();File[] files=root.listFiles();if(files!=null)for(File f:files)if(valid(f.getName()))remove(f.getName());}
    private synchronized void store(String name,byte[] bytes,int generation){
        if(epoch.get()!=generation)return;
        File temp=file(name+".tmp");
        try{
            if(!root.isDirectory()&&!root.mkdirs())return;
            try(OutputStream out=new FileOutputStream(temp)){out.write(bytes);}
            if(!temp.renameTo(file(name)))return;
            try(OutputStream out=new FileOutputStream(file(name+".sha256"))){out.write(hash(bytes));}
            if(stats()[0]>limit){File[] files=root.listFiles();if(files!=null){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files){if(valid(f.getName()))remove(f.getName());if(stats()[0]<=limit*.8)break;}}}
        }catch(Exception ignored){/* Read-only/full storage: display bytes without persistence. */}
        finally{temp.delete();}
    }
    public byte[] get(String name)throws Exception{
        if(!valid(name))throw new IOException("Invalid filename");
        final int generation=epoch.get();
        FutureTask<byte[]> work=new FutureTask<>(()->{
            slots.acquire();
            try{
                byte[] bytes=cached(name);if(bytes!=null)return bytes;
                if(failures.getOrDefault(name,0L)>System.currentTimeMillis())throw new IOException("Retry later");
                bytes=downloader.load(name);if(bytes.length>MAX_FILE)throw new IOException("Image too large");mime(bytes);store(name,bytes,generation);return bytes;
            }catch(Exception e){if(failures.size()>4096)failures.clear();failures.put(name,System.currentTimeMillis()+30000);throw e;}
            finally{slots.release();}
        });
        FutureTask<byte[]> existing=pending.putIfAbsent(name,work);
        if(existing!=null)return existing.get();
        try{work.run();return work.get();}finally{pending.remove(name,work);}
    }
}
