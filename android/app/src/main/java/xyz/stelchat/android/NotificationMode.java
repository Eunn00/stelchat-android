package xyz.stelchat.android;

final class NotificationMode {
    static String resolve(String mode, boolean legacySound) {
        if(mode==null||mode.isEmpty())return legacySound?"sound":"silent";
        return mode.equals("sound")||mode.equals("vibrate")||mode.equals("silent")||mode.equals("none")?mode:"sound";
    }
    static String channel(String kind,String mode){return "stelchat_v2_"+kind+"_"+mode;}
}
