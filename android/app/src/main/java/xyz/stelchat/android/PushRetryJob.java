package xyz.stelchat.android;
import android.app.job.*;

public class PushRetryJob extends JobService {
    @Override public boolean onStartJob(JobParameters params){
        PushSubscriptions.request(this,false);
        // A one-shot persisted job survives process death; network constraints avoid offline loops.
        PushSubscriptions.retryLater(this,300000);
        return false;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
