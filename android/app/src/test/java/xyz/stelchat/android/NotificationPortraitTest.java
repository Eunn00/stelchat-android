package xyz.stelchat.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class NotificationPortraitTest {
 @Test public void onlyVersionedMemberPortraitsAreAcceptedForChat() {
  String uid="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  assertTrue(NotificationPortrait.allowedPath("/api/profile-images/"+uid+"?v=abcdef123456",uid,"chat"));
  assertTrue(NotificationPortrait.allowedPath("/api/profile-images/"+uid+"?v=abcdef123456",uid,"live"));
  assertFalse(NotificationPortrait.allowedPath("https://evil.test/image",uid,"chat"));
  assertFalse(NotificationPortrait.allowedPath("/api/profile-images/"+uid+"?v=abcdef123456","bad","chat"));
  assertEquals(NotificationPortrait.ownerKey(uid,"chat"),NotificationPortrait.ownerKey(uid,"live"));
 }
}
