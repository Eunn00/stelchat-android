package xyz.stelchat.android;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private EmojiDiskCache emojiCache;
    public EmojiDiskCache emojiCache(){return emojiCache;}
    @Override public void onCreate(android.os.Bundle state) {
        registerPlugin(StelChatNativePlugin.class);
        super.onCreate(state);
        emojiCache=new EmojiDiskCache(new java.io.File(getCacheDir(),"emoji-v1"));
        bridge.setWebViewClient(new com.getcapacitor.BridgeWebViewClient(bridge){
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view,android.webkit.WebResourceRequest request){
                android.net.Uri uri=request.getUrl();
                if("https".equals(uri.getScheme())&&"localhost".equals(uri.getHost())&&uri.getPath().startsWith("/emoji-cache/")){
                    try{
                        String name=uri.getPath().substring("/emoji-cache/".length());
                        if(!"GET".equals(request.getMethod())||uri.getQuery()!=null||!EmojiDiskCache.valid(name))throw new java.io.IOException();
                        byte[] bytes=emojiCache.get(name);
                        return new android.webkit.WebResourceResponse(EmojiDiskCache.mime(bytes),null,200,"OK",java.util.Collections.singletonMap("Cache-Control","no-store"),new java.io.ByteArrayInputStream(bytes));
                    }catch(Exception e){return new android.webkit.WebResourceResponse("text/plain","UTF-8",503,"Unavailable",java.util.Collections.emptyMap(),new java.io.ByteArrayInputStream(new byte[0]));}
                }
                return super.shouldInterceptRequest(view,request);
            }
        });
        if((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)
            android.webkit.WebView.setWebContentsDebuggingEnabled(true);
    }
}
