package xyz.stelchat.android;

import java.util.Set;
import java.util.TreeSet;

/** Pure reconciliation policy, shared by production and regression tests. */
final class PushSubscriptionPlan {
    static final class Operation {
        final String topic;
        final boolean subscribe;
        Operation(String topic, boolean subscribe) { this.topic=topic; this.subscribe=subscribe; }
    }
    static Operation next(Set<String> desired, Set<String> managed, Set<String> confirmed, Set<String> verified) {
        for(String topic:new TreeSet<>(managed))
            if(!desired.contains(topic))return new Operation(topic,false);
        for(String topic:new TreeSet<>(desired))
            if(!confirmed.contains(topic)||!verified.contains(topic))return new Operation(topic,true);
        return null;
    }
}
