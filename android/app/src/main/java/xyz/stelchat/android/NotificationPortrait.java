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
    static boolean allowedPath(String path,String uid,String kind){
        if(path==null||uid==null||!uid.matches("[a-f0-9]{32}"))return false;
        if("tweet".equals(kind)||"space".equals(kind))return path.matches("/x-api/image/[a-f0-9]{64}\\?portrait=160-v2");
        return ("chat".equals(kind)||"live".equals(kind))&&path.matches("/api/profile-images/"+uid+"\\?v=[a-f0-9]{12}");
    }
    static String ownerKey(String uid,String kind){
        if(uid==null||!uid.matches("[a-f0-9]{32}"))return null;
        if("tweet".equals(kind)||"space".equals(kind))return uid+"-x";
        if("chat".equals(kind)||"live".equals(kind))return uid+"-chzzk";
        return null;
    }
    private static File root(Context context){return new File(context.getCacheDir(),"notification-portraits-v1");}
    private static Bitmap fallback(Context context,String uid,String kind){
        byte[] bytes=PortraitCacheStore.read(root(context),ownerKey(uid,kind));
        return bytes==null?null:decode(bytes);
    }
    static Bitmap load(Context context,String path,String uid,String kind){
        if(!allowedPath(path,uid,kind))return fallback(context,uid,kind);
        HttpURLConnection connection=null;
        try{
            File root=root(context);
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder key=new StringBuilder();for(byte b:digest)key.append(String.format("%02x",b));
            byte[] cached=PortraitCacheStore.read(root,key.toString());            if(cached!=null&&cached.length<=PortraitCacheStore.MAX_BYTES){
                Bitmap hit=decode(cached);
                if(hit!=null){PortraitCacheStore.write(root,ownerKey(uid,kind),cached);return hit;}
            }
            connection=(HttpURLConnection)new URL("https://stelchat.xyz"+path).openConnection();
            connection.setConnectTimeout(1500);connection.setReadTimeout(1500);connection.setInstanceFollowRedirects(false);
            if(connection.getResponseCode()!=200||connection.getContentLengthLong()>262144)return fallback(context,uid,kind);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=connection.getInputStream()){
                byte[] block=new byte[4096];int n;long deadline=System.nanoTime()+2000000000L;
                while((n=input.read(block))!=-1){if(out.size()+n>262144||System.nanoTime()>deadline)return fallback(context,uid,kind);out.write(block,0,n);}
            }
            byte[] bytes=out.toByteArray();Bitmap result=decode(bytes);if(result==null)return fallback(context,uid,kind);
            PortraitCacheStore.write(root,key.toString(),bytes);
            PortraitCacheStore.write(root,ownerKey(uid,kind),bytes);
            return result;
        }catch(Exception ignored){return fallback(context,uid,kind);}finally{if(connection!=null)connection.disconnect();}
    }
}
