package com.fsck.k9.service;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import timber.log.Timber;
import com.fsck.k9.K9;

public class PushService extends CoreService {
    private static String START_SERVICE = "com.fsck.k9.service.PushService.startService";
    private static String STOP_SERVICE = "com.fsck.k9.service.PushService.stopService";

    public static void startService(Context context) {
        Intent i = new Intent();
        i.setClass(context, PushService.class);
        i.setAction(PushService.START_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    public static void stopService(Context context) {
        Intent i = new Intent();
        i.setClass(context, PushService.class);
        i.setAction(PushService.STOP_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    @Override
    public int startService(Intent intent, int startId) {
        int startFlag = START_STICKY;
        if (START_SERVICE.equals(intent.getAction())) {
            Timber.i("PushService started with startId = %d", startId);
        } else if (STOP_SERVICE.equals(intent.getAction())) {
            Timber.i("PushService stopping with startId = %d", startId);
            stopSelf(startId);
            startFlag = START_NOT_STICKY;
        }

        return startFlag;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setAutoShutdown(false);
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
