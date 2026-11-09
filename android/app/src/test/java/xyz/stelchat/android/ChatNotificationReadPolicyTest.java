package xyz.stelchat.android;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatNotificationReadPolicyTest {
    private static final String UID="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    @Test public void includesOlderButNotNewMessages(){
        assertTrue(ChatNotificationReadPolicy.covers("10","9"));
        assertTrue(ChatNotificationReadPolicy.covers("10","10"));
        assertFalse(ChatNotificationReadPolicy.covers("10","11"));
    }
    @Test public void largeIdsDoNotLosePrecision(){
        assertTrue(ChatNotificationReadPolicy.covers("90071992547409931","90071992547409930"));
        assertEquals("90071992547409931",ChatNotificationReadPolicy.newest("90071992547409930","90071992547409931"));
    }
    @Test public void onlyExactMemberChatKeysMatch(){
        assertEquals("10",ChatNotificationReadPolicy.messageId("chat:"+UID+":10",UID));
        assertEquals("",ChatNotificationReadPolicy.messageId("chat:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb:10",UID));
        assertEquals("",ChatNotificationReadPolicy.messageId("x:"+UID+":10",UID));
        assertEquals("",ChatNotificationReadPolicy.messageId("chat:"+UID+":invalid",UID));
    }
    @Test public void malformedWatermarksCannotHideAlerts(){
        assertFalse(ChatNotificationReadPolicy.covers("","1"));
        assertFalse(ChatNotificationReadPolicy.covers("1","0"));
        assertFalse(ChatNotificationReadPolicy.covers("-1","1"));
        assertEquals("10",ChatNotificationReadPolicy.newest("10","9"));
    }
}
