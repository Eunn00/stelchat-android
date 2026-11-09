package xyz.stelchat.android;

import java.math.BigInteger;

/** Numeric DB identifiers only; never compare notification IDs lexically. */
public final class ChatNotificationReadPolicy {
    public static boolean validId(String value){return value!=null&&value.matches("[1-9][0-9]{0,19}");}
    public static boolean validUid(String uid){return uid!=null&&uid.matches("[a-f0-9]{32}");}
    public static String messageId(String key,String uid){
        String prefix="chat:"+uid+":";
        if(!validUid(uid)||key==null||!key.startsWith(prefix))return "";
        String id=key.substring(prefix.length());return validId(id)?id:"";
    }
    public static boolean covers(String through,String id){
        return validId(through)&&validId(id)&&new BigInteger(id).compareTo(new BigInteger(through))<=0;
    }
    public static String newest(String first,String second){
        if(!validId(first))return validId(second)?second:"";
        return covers(first,second)?first:validId(second)?second:first;
    }
}
