package xyz.stelchat.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded, private thumbnail cache. No arbitrary URLs and no original-size downloads. */
final class NotificationPortrait {
    static Bitmap decode(byte[] bytes){
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>512||bounds.outHeight>512)return null;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        return bitmap==null?null:Bitmap.createScaledBitmap(bitmap,160,160,true);
    }
    static Bitmap load(Context context,String path,String uid){
        if(!path.matches("/api/profile-images/"+uid+"\\?v=[a-f0-9]{12}"))return null;
        HttpURLConnection connection=null;
        try{
            File root=new File(context.getCacheDir(),"notification-portraits-v1");root.mkdirs();
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder key=new StringBuilder();for(byte b:digest)key.append(String.format("%02x",b));
            File file=new File(root,key.toString());
            if(file.isFile()&&file.length()<=262144){
                Bitmap hit=decode(java.nio.file.Files.readAllBytes(file.toPath()));
                if(hit!=null){file.setLastModified(System.currentTimeMillis());return hit;}
                file.delete();
            }
            connection=(HttpURLConnection)new URL("https://stelchat.xyz"+path).openConnection();
            connection.setConnectTimeout(1500);connection.setReadTimeout(1500);connection.setInstanceFollowRedirects(false);
            if(connection.getResponseCode()!=200||connection.getContentLengthLong()>262144)return null;
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=connection.getInputStream()){
                byte[] block=new byte[4096];int n;long deadline=System.nanoTime()+2000000000L;
                while((n=input.read(block))!=-1){if(out.size()+n>262144||System.nanoTime()>deadline)return null;out.write(block,0,n);}
            }
            byte[] bytes=out.toByteArray();Bitmap result=decode(bytes);if(result==null)return null;
            try{
                java.nio.file.Files.write(file.toPath(),bytes);
                File[] files=root.listFiles();if(files!=null){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-32;i++)files[i].delete();}
            }catch(IOException ignored){/* Display downloaded portrait even if disk is full. */}
            return result;
        }catch(Exception ignored){return null;}finally{if(connection!=null)connection.disconnect();}
    }
}
