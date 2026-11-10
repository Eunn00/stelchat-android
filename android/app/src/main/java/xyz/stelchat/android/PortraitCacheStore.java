package xyz.stelchat.android;

import java.io.*;
import java.util.*;

/** Bounded atomic portrait files shared by foreground/background notification callbacks. */
final class PortraitCacheStore {
    static final int MAX_BYTES=262144,MAX_FILES=64;
    static boolean valid(String key){return key!=null&&(key.matches("[a-f0-9]{64}")||key.matches("[a-f0-9]{32}-(x|chzzk)"));}
    static synchronized byte[] read(File root,String key){
        if(!valid(key))return null;
        File file=new File(root,key);
        if(!file.isFile()||file.length()<12||file.length()>MAX_BYTES)return null;
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[4096];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>MAX_BYTES)return null;out.write(block,0,n);}
            file.setLastModified(System.currentTimeMillis());return out.toByteArray();
        }catch(IOException e){return null;}
    }
    static synchronized void write(File root,String key,byte[] bytes){
        if(!valid(key)||bytes==null||bytes.length<12||bytes.length>MAX_BYTES)return;
        File temporary=null;
        try{
            if(!root.isDirectory()&&!root.mkdirs())return;
            temporary=new File(root,"."+key+"."+UUID.randomUUID()+".tmp");
            try(OutputStream out=new FileOutputStream(temporary)){out.write(bytes);}
            File destination=new File(root,key);
            if(!temporary.renameTo(destination)){
                // Java File.renameTo cannot replace an existing file on Windows
                // (unit tests). Never delete the previous valid portrait first.
                try{java.nio.file.Files.move(temporary.toPath(),destination.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}
                catch(java.nio.file.AtomicMoveNotSupportedException e){return;}
                catch(LinkageError e){return;}
            }
            File[] files=root.listFiles(f->valid(f.getName()));
            if(files!=null&&files.length>MAX_FILES){
                Arrays.sort(files,Comparator.comparingLong(File::lastModified));
                for(int i=0;i<files.length-MAX_FILES;i++)if(!files[i].getName().equals(key))files[i].delete();
            }
        }catch(IOException ignored){/* Full cache storage must not prevent a text notification. */}
        finally{if(temporary!=null)temporary.delete();}
    }
}
