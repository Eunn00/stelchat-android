package xyz.stelchat.android;

import android.content.Context;
import org.json.JSONArray;
import java.util.LinkedHashSet;

/** Exact event keys only: viewing an older page must not dismiss later arrivals. */
public final class NotificationReadState {
    public static synchronized boolean contains(Context c,String key){return keys(c).contains(key);}
    private static LinkedHashSet<String> keys(Context c){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        try{JSONArray a=new JSONArray(c.getSharedPreferences("stelchat-push",0).getString("read_keys","[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}
        return out;
    }
    public static synchronized void acknowledge(Context c,JSONArray values){
        LinkedHashSet<String> all=keys(c);
        for(int i=0;i<values.length();i++){String key=values.optString(i);if(key.length()>0&&key.length()<=180){all.add(key);androidx.core.app.NotificationManagerCompat.from(c).cancel(key.hashCode());}}
        while(all.size()>2000)all.remove(all.iterator().next());
        c.getSharedPreferences("stelchat-push",0).edit().putString("read_keys",new JSONArray(all).toString()).commit();
    }
}
