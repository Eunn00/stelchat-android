package xyz.stelchat.android;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

/** Exact event keys plus room/member read boundaries; never dismiss later arrivals. */
public final class NotificationReadState {
    public static synchronized boolean containsMessage(Context c,String key,String kind,String uid,String sessionId){
        if(contains(c,key))return true;
        if(!"chat".equals(kind)||!ChatNotificationReadPolicy.validId(sessionId))return false;
        try{
            JSONObject marks=new JSONObject(c.getSharedPreferences("stelchat-push",0).getString("chat_read_through","{}"));
            return ChatNotificationReadPolicy.covers(marks.optString(sessionId+":"+uid),ChatNotificationReadPolicy.messageId(key,uid));
        }catch(Exception ignored){return false;}
    }
    public static synchronized void acknowledgeChatRooms(Context c,JSONArray rooms,String scope,String memberUid){
        android.content.SharedPreferences storage=c.getSharedPreferences("stelchat-push",0);
        JSONObject marks;
        try{marks=new JSONObject(storage.getString("chat_read_through","{}"));}catch(Exception ignored){marks=new JSONObject();}
        for(int i=0;i<rooms.length();i++){
            JSONObject room=rooms.optJSONObject(i);if(room==null)continue;
            putMark(marks,room.optString("uid"),room.optString("session_id"),room.optString("through_id"));
        }
        JSONArray removed=new JSONArray();
        android.app.NotificationManager manager=c.getSystemService(android.app.NotificationManager.class);
        for(android.service.notification.StatusBarNotification item:manager.getActiveNotifications()){
            android.os.Bundle extras=item.getNotification().extras;
            String key=extras.getString("stelchat_event_key","");
            String uid=extras.getString("stelchat_uid",""),sid=extras.getString("stelchat_session_id","");
            // Older app versions only saved the event key. Bulk actions can
            // still safely identify their member, but never guess their room.
            if(uid.isEmpty()&&key.length()>38&&key.startsWith("chat:"))uid=key.substring(5,37);
            String id=ChatNotificationReadPolicy.messageId(key,uid);
            if(id.isEmpty())continue;
            boolean bulk="all".equals(scope)||("member".equals(scope)&&uid.equals(memberUid));
            if(bulk)putMark(marks,uid,sid,id);
            if(bulk||ChatNotificationReadPolicy.covers(marks.optString(sid+":"+uid),id)){
                removed.put(key);manager.cancel(item.getTag(),item.getId());
            }
        }
        while(marks.length()>2000)marks.remove(marks.keys().next());
        storage.edit().putString("chat_read_through",marks.toString()).commit();
        acknowledge(c,removed);
    }
    private static void putMark(JSONObject marks,String uid,String sid,String id){
        if(!ChatNotificationReadPolicy.validUid(uid)||!ChatNotificationReadPolicy.validId(sid)||!ChatNotificationReadPolicy.validId(id))return;
        String key=sid+":"+uid;
        try{marks.put(key,ChatNotificationReadPolicy.newest(marks.optString(key),id));}catch(Exception ignored){}
    }
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
