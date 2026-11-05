package xyz.stelchat.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;

public class NotificationModeTest {
    @Test public void modesAndLegacySettings(){
        assertEquals("silent",NotificationMode.resolve("",false));
        assertEquals("sound",NotificationMode.resolve(null,true));
        assertEquals("sound",NotificationMode.resolve("invalid",false));
        for(String mode:new String[]{"sound","vibrate","silent","none"})assertEquals(mode,NotificationMode.resolve(mode,false));
    }
    @Test public void eachEventAndModeHasIndependentImmutableAndroidChannel(){
        HashSet<String> channels=new HashSet<>();
        for(String kind:new String[]{"live","chat"})for(String mode:new String[]{"sound","vibrate","silent"})channels.add(NotificationMode.channel(kind,mode));
        assertEquals(6,channels.size());
        assertFalse(channels.contains("stelchat_live_sound"));
    }
}
