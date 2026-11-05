package xyz.stelchat.android;

import android.app.job.*;
import android.content.*;
import android.os.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.*;
import java.util.*;

/** One operation in flight; commit each acknowledgment, then reconcile latest settings. */
final class PushSubscriptions {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final java.util.concurrent.ExecutorService REGISTRATION=java.util.concurrent.Executors.newSingleThreadExecutor();
    static com.google.android.gms.tasks.Task<Void> enableRegistration(){
        return com.google.android.gms.tasks.Tasks.call(REGISTRATION,()->{
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);return (Void)null;
        }).onSuccessTask(ignored->FirebaseMessaging.getInstance().register());
    }
    private static final int JOB=71031;
    private static boolean busy=false, initialized=false;
    private static long tokenGeneration=0;
    private static final Set<String> verified=new HashSet<>();
    static Set<String> desired(Context context){
        Set<String> out=new TreeSet<>();JSONObject config=StelChatMessagingService.settings(context);
        if("none".equals(config.optString("notificationMode")))return out;
        JSONObject all=config.optJSONObject("notificationPreferences");
        if(all!=null)for(Iterator<String> i=all.keys();i.hasNext();){String uid=i.next();
            if(!uid.matches("[a-f0-9]{32}"))continue;
            JSONObject member=all.optJSONObject(uid),desktop=member==null?null:member.optJSONObject("desktop");
            for(String kind:new String[]{"live","chat"})if(desktop!=null&&desktop.optBoolean(kind))out.add("stelchat_"+kind+"_"+uid);
        }return out;
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("stelchat-push",0);}
    static void request(Context context,boolean newToken){
        Context app=context.getApplicationContext();
        MAIN.post(()->{if(newToken){tokenGeneration++;verified.clear();}reconcile(app);});
    }
    private static Set<String> saved(Context c,String key){return new HashSet<>(prefs(c).getStringSet(key,Collections.emptySet()));}
    private static void state(Context c,String value,String error){prefs(c).edit().putString("sync_state",value).putString("sync_error",error).putLong("sync_updated",System.currentTimeMillis()).apply();}
    static void retryLater(Context c,long delay){
        c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(JOB,new ComponentName(c,PushRetryJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(delay).setPersisted(true).build());
    }
    private static void reconcile(Context c){
        if(FirebaseApp.getApps(c).isEmpty())return;
        if(!initialized){initialized=true;verified.clear();}
        Set<String> want=desired(c),known=saved(c,"managed_topics"),confirmed=saved(c,"topics");
        known.addAll(confirmed);known.addAll(want);
        prefs(c).edit().putStringSet("managed_topics",known).apply();
        if(busy){state(c,"syncing","");return;}
        PushSubscriptionPlan.Operation operation=PushSubscriptionPlan.next(want,known,confirmed,verified);
        if(operation==null){state(c,"synced","");c.getSystemService(JobScheduler.class).cancel(JOB);return;}
        busy=true;state(c,"syncing","");retryLater(c,60000);
        String topic=operation.topic;boolean adding=operation.subscribe;long generation=tokenGeneration;
        FirebaseMessaging fcm=FirebaseMessaging.getInstance();
        // No timeout re-enqueue: the SDK owns the durable retry queue for this operation.
        enableRegistration().onSuccessTask(ignored->adding?fcm.subscribeToTopic(topic):fcm.unsubscribeFromTopic(topic)).addOnCompleteListener(task->{
            busy=false;
            if(task.isSuccessful()){
                Set<String> done=saved(c,"topics"),managed=saved(c,"managed_topics");
                if(adding){done.add(topic);if(generation==tokenGeneration)verified.add(topic);}else{done.remove(topic);managed.remove(topic);verified.remove(topic);}
                prefs(c).edit().putStringSet("topics",done).putStringSet("managed_topics",managed).commit();
                reconcile(c);
            }else{
                String reason=task.getException()==null?"Unknown":task.getException().getClass().getSimpleName();
                state(c,"retrying",reason);android.util.Log.w("StelChatPush","Topic synchronization failed: "+reason);
                retryLater(c,60000);MAIN.postDelayed(()->reconcile(c),60000);
            }
        });
    }
    static JSONObject status(Context c){
        JSONObject out=new JSONObject();try{
            Set<String> want=desired(c),have=saved(c,"topics");int matched=0;for(String t:want)if(have.contains(t))matched++;
            String state=prefs(c).getString("sync_state","pending");
            boolean synced="synced".equals(state)&&have.equals(want);
            out.put("synced",synced);out.put("syncState",state);out.put("desiredTopics",want.size());out.put("confirmedTopics",matched);
            out.put("syncError",prefs(c).getString("sync_error",""));
        }catch(JSONException ignored){}return out;
    }
}
