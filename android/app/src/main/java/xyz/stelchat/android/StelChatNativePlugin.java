package xyz.stelchat.android;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@CapacitorPlugin(name="StelChatNative", permissions={@Permission(alias="notifications",strings={Manifest.permission.POST_NOTIFICATIONS})})
public class StelChatNativePlugin extends Plugin {
    @PluginMethod public void consumeNotificationOpen(PluginCall call){
        JSObject result=new JSObject();Intent intent=getActivity().getIntent();
        String uid=intent.getStringExtra("uid"),kind=intent.getStringExtra("kind");
        if(uid!=null&&uid.matches("[a-f0-9]{32}")&&java.util.Arrays.asList("chat","live").contains(kind)){
            for(String key:new String[]{"uid","kind","event_key","session_id"}){result.put(key,intent.getStringExtra(key));intent.removeExtra(key);}
        }
        call.resolve(result);
    }
    @PluginMethod public void acknowledgeNotifications(PluginCall call){
        JSArray keys=call.getArray("keys");if(keys==null||keys.length()>500){call.reject("Invalid read keys");return;}
        NotificationReadState.acknowledge(getContext(),keys);call.resolve();
    }
    @Override protected void handleOnNewIntent(Intent intent){notifyListeners("notificationOpen",new JSObject(),true);}
    private EmojiDiskCache emojiCache(){return ((MainActivity)getActivity()).emojiCache();}
    @PluginMethod public void emojiCacheStats(PluginCall call){long[] stats=emojiCache().stats();JSObject result=new JSObject();result.put("bytes",stats[0]);result.put("limit",stats[1]);result.put("count",stats[2]);call.resolve(result);}
    @PluginMethod public void emojiCacheClear(PluginCall call){emojiCache().clear();emojiCacheStats(call);}
    @PluginMethod public void emojiCacheHas(PluginCall call){
        try{JSArray files=call.getArray("files");if(files==null||files.length()>64)throw new Exception();JSArray found=new JSArray();for(int i=0;i<files.length();i++){String file=files.getString(i);if(!EmojiDiskCache.valid(file))throw new Exception();if(emojiCache().has(file))found.put(file);}JSObject result=new JSObject();result.put("files",found);call.resolve(result);}catch(Exception e){call.reject("Invalid cache request");}
    }
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final AtomicInteger generation=new AtomicInteger();
    private volatile HttpURLConnection connection;
    @PluginMethod public void readSettings(PluginCall call) {
        JSObject response=new JSObject();
        response.put("settings",StelChatMessagingService.settings(getContext()));call.resolve(response);
    }
    @PluginMethod public void openExternal(PluginCall call) {
        String value=call.getString("url","");
        try {
            URI uri=new URI(value);
            if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null||uri.getPort()!=-1||
              !("stelchat.xyz".equals(uri.getHost())||"chzzk.naver.com".equals(uri.getHost())))throw new Exception();
            getActivity().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(value)));call.resolve();
        }catch(Exception e){call.reject("Invalid or unavailable external link");}
    }
    private boolean configured(){return !FirebaseApp.getApps(getContext()).isEmpty();}
    @PluginMethod public void openNotificationSettings(PluginCall call) {
        Intent intent=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getContext().getPackageName());
        getActivity().startActivity(intent);call.resolve();
    }
    @PluginMethod public void pushStatus(PluginCall call) {
        JSObject response=new JSObject();response.put("configured",configured());
        org.json.JSONObject sync=PushSubscriptions.status(getContext());
        for(java.util.Iterator<String> i=sync.keys();i.hasNext();){String key=i.next();response.put(key,sync.opt(key));}
        response.put("permission",Build.VERSION.SDK_INT<33||getPermissionState("notifications")==PermissionState.GRANTED);call.resolve(response);
    }
    @PluginMethod public void retryPush(PluginCall call){PushSubscriptions.request(getContext(),false);call.resolve();}
    @PluginMethod public void enablePush(PluginCall call) {
        if(!configured()){call.reject("알림 서비스를 사용할 수 없습니다. 앱 업데이트를 확인해 주세요.");return;}
        if(Build.VERSION.SDK_INT>=33&&getPermissionState("notifications")!=PermissionState.GRANTED){requestPermissionForAlias("notifications",call,"permissionResult");return;}
        enableToken(call);
    }
    @PermissionCallback private void permissionResult(PluginCall call) {
        if(getPermissionState("notifications")!=PermissionState.GRANTED){call.reject("알림 권한이 필요합니다.");return;}
        enableToken(call);
    }
    private void enableToken(PluginCall call) {
        PushSubscriptions.enableRegistration().addOnCompleteListener(task->{
            if(task.isSuccessful()){JSObject r=new JSObject();r.put("enabled",true);call.resolve(r);}
            else call.reject("알림 등록에 실패했습니다. 네트워크 연결을 확인하고 다시 시도해 주세요.");
        });
    }
    @PluginMethod public void savePushSettings(PluginCall call) {
        JSObject settings=call.getObject("settings");
        if(settings==null||settings.toString().length()>65536){call.reject("Invalid settings");return;}
        if(!getContext().getSharedPreferences("stelchat-push",0).edit().putString("settings",settings.toString()).commit()){call.reject("설정 저장 실패");return;}
        if(configured())PushSubscriptions.request(getContext(),false);
        call.resolve();
    }
    private void stopStream(){generation.incrementAndGet();HttpURLConnection c=connection;if(c!=null)c.disconnect();}
    @PluginMethod public void stopEvents(PluginCall call){stopStream();call.resolve();}
    @PluginMethod public void startEvents(PluginCall call){
        stopStream();final int current=generation.get();
        executor.submit(()->readStream(current));call.resolve();
    }
    private void readStream(int current){
        HttpURLConnection c=null;
        try{
            if(current!=generation.get())return;
            c=(HttpURLConnection)new URL("https://stelchat.xyz/api/events").openConnection();connection=c;
            c.setConnectTimeout(15000);c.setReadTimeout(55000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","text/event-stream");
            if(c.getResponseCode()!=200)throw new IOException("SSE status");
            if(current!=generation.get())return;
            JSObject state=new JSObject();state.put("connected",true);notifyListeners("streamState",state);
            try(Reader reader=new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8)){
                StringBuilder block=new StringBuilder();int character;
                while(current==generation.get()&&(character=reader.read())!=-1){
                    if(character=='\r')continue;
                    block.append((char)character);
                    if(block.length()>262144)throw new IOException("Oversized event");
                    int n=block.length();
                    if(n>=2&&block.charAt(n-1)=='\n'&&block.charAt(n-2)=='\n'){
                        JSObject event=new JSObject();event.put("block",block.toString());notifyListeners("sse",event);block.setLength(0);
                    }
                }
            }
        }catch(Exception ignored){/* Generic state only: no credentials or chat content in logs. */}
        finally{
            if(c!=null)c.disconnect();
            if(current==generation.get()){connection=null;JSObject state=new JSObject();state.put("connected",false);notifyListeners("streamState",state);}
        }
    }
    @Override protected void handleOnPause(){stopStream();}
    @Override protected void handleOnResume(){if(configured())PushSubscriptions.request(getContext(),false);}
    @Override protected void handleOnDestroy(){stopStream();executor.shutdownNow();}
}
