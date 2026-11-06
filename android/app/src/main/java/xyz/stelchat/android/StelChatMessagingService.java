package xyz.stelchat.android;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.provider.Settings;
import android.graphics.Bitmap;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.messaging.*;
import com.google.android.gms.tasks.*;
import org.json.*;
import java.util.*;

public class StelChatMessagingService extends FirebaseMessagingService {
    public interface Completion {void done(boolean success);}
    public static JSONObject settings(Context context){
        try{return new JSONObject(context.getSharedPreferences("stelchat-push",0).getString("settings","{}"));}catch(Exception e){return new JSONObject();}
    }
    public static void syncTopics(Context context,boolean newToken,Completion callback){
        PushSubscriptions.request(context,newToken);
        callback.done(false); // Completion is reported by pushStatus, never by settings persistence.
    }
    @Override public void onNewToken(String token){syncTopics(this,true,success->{});}
    @Override public void onRegistered(String installationId){
        android.content.SharedPreferences p=getSharedPreferences("stelchat-push",0);
        boolean changed=!installationId.equals(p.getString("registration_id",""));
        p.edit().putString("registration_id",installationId).apply();
        PushSubscriptions.request(this,changed);
    }
    @Override public void onMessageReceived(RemoteMessage message){
        Map<String,String> data=message.getData();
        String uid=data.getOrDefault("uid",""),kind=data.getOrDefault("kind",""),key=data.getOrDefault("event_key","");
        if(!uid.matches("[a-f0-9]{32}")||!(kind.equals("chat")||kind.equals("live"))||key.isEmpty()||key.length()>180)return;
        try{
            long age=System.currentTimeMillis()-Long.parseLong(data.getOrDefault("sent_at_ms","0"));
            if(age < -60000 || age>600000)return;
            JSONObject config=settings(this),preferences=config.optJSONObject("notificationPreferences");
            // Also reject messages already queued before topic unsubscription.
            if("none".equals(config.optString("notificationMode")))return;
            JSONObject member=preferences==null?null:preferences.optJSONObject(uid);
            JSONObject desktop=member==null?null:member.optJSONObject("desktop"),sound=member==null?null:member.optJSONObject("sound");
            if(desktop==null||!desktop.optBoolean(kind))return;
            if(NotificationReadState.contains(this,key))return;
            JSONObject muted=config.optJSONObject("mutedChatRooms");
            if(kind.equals("chat")&&muted!=null&&muted.optBoolean(data.getOrDefault("session_id","")+":"+uid))return;
            NotificationManagerCompat manager=NotificationManagerCompat.from(this);
            if(!manager.areNotificationsEnabled())return;
            SharedPreferences storage=getSharedPreferences("stelchat-push",0);
            synchronized(StelChatMessagingService.class){
                JSONArray recent=new JSONArray(storage.getString("seen","[]"));
                for(int i=0;i<recent.length();i++)if(key.equals(recent.getString(i)))return;
                JSONArray next=new JSONArray();for(int i=Math.max(0,recent.length()-199);i<recent.length();i++)next.put(recent.getString(i));next.put(key);
                storage.edit().putString("seen",next.toString()).commit();
            }
            String mode=NotificationMode.resolve(config.optString("notificationMode",""),sound!=null&&sound.optBoolean(kind));
            boolean audible=mode.equals("sound"),vibrate=mode.equals("vibrate");
            String channelId=NotificationMode.channel(kind,mode);
            if(android.os.Build.VERSION.SDK_INT>=26){
                NotificationChannel channel=new NotificationChannel(channelId,(kind.equals("chat")?"새 채팅":"방송 시작")+(audible?" · 소리":vibrate?" · 진동":" · 무음"),mode.equals("silent")?NotificationManager.IMPORTANCE_LOW:NotificationManager.IMPORTANCE_HIGH);
                channel.setSound(audible?Settings.System.DEFAULT_NOTIFICATION_URI:null,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                channel.enableVibration(vibrate);
                if(vibrate)channel.setVibrationPattern(new long[]{0,250,120,250});
                getSystemService(NotificationManager.class).createNotificationChannel(channel);
            }
            Intent intent=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setData(android.net.Uri.parse("stelchat://notification/"+android.net.Uri.encode(key)));
            for(String field:new String[]{"uid","kind","event_key","session_id"})intent.putExtra(field,data.getOrDefault(field,""));
            PendingIntent content=PendingIntent.getActivity(this,key.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            String title=data.getOrDefault("title","StelChat"),body=data.getOrDefault("body","");
            NotificationCompat.Builder notification=new NotificationCompat.Builder(this,channelId)
                .setSmallIcon(R.drawable.ic_stat_stelchat).setContentTitle(title).setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true)
                .setContentIntent(content).setPriority(mode.equals("silent")?NotificationCompat.PRIORITY_LOW:NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true);
            if(audible)notification.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
            else notification.setSound(null);
            if(vibrate)notification.setVibrate(new long[]{0,250,120,250});
            else notification.setVibrate(new long[]{0});
            if(mode.equals("silent"))notification.setSilent(true);
            if(NotificationReadState.contains(this,key))return;
            manager.notify(key.hashCode(),notification.build());
            Bitmap portrait=NotificationPortrait.load(this,data.getOrDefault("avatar_path",""),uid);
            if(portrait!=null){
                // Do not resurrect a notification dismissed during the download.
                boolean visible=false;
                for(android.service.notification.StatusBarNotification item:getSystemService(NotificationManager.class).getActiveNotifications())
                    if(item.getId()==key.hashCode())visible=true;
                if(visible&&!NotificationReadState.contains(this,key))manager.notify(key.hashCode(),notification.setLargeIcon(portrait).build());
            }
        }catch(Exception ignored){/* Invalid push payloads never crash the app. */}
    }
}
