package xyz.stelchat.android;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PushSubscriptionPlanTest {
    private Set<String> set(String... values){return new HashSet<>(Arrays.asList(values));}
    @Test public void initialSubscriptionIsNotAlreadyConfirmed(){
        assertTrue(PushSubscriptionPlan.next(set("chat"),set("chat"),set(),set()).subscribe);
    }
    @Test public void completedOperationsAreNotRepeated(){
        assertNull(PushSubscriptionPlan.next(set("chat"),set("chat"),set("chat"),set("chat")));
    }
    @Test public void restartRechecksPreviouslySavedSubscriptions(){
        assertTrue(PushSubscriptionPlan.next(set("chat"),set("chat"),set("chat"),set()).subscribe);
    }
    @Test public void interruptedBatchContinuesOnlyRemainingItems(){
        assertEquals("live",PushSubscriptionPlan.next(set("chat","live"),set("chat","live"),set("chat"),set("chat")).topic);
    }
    @Test public void switchedOffDuringRequestRemovesJustCompletedSubscription(){
        assertFalse(PushSubscriptionPlan.next(set(),set("chat"),set("chat"),set("chat")).subscribe);
    }
    @Test public void switchedOnDuringUnsubscribeIsSubscribedAgain(){
        assertTrue(PushSubscriptionPlan.next(set("chat"),set(),set(),set()).subscribe);
    }
    @Test public void removesObsoleteTopicBeforeAddingNewOne(){
        PushSubscriptionPlan.Operation op=PushSubscriptionPlan.next(set("new"),set("old","new"),set("old"),set("old"));
        assertEquals("old",op.topic);assertFalse(op.subscribe);
    }
}
