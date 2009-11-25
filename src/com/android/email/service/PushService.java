package com.android.email.service;

import com.android.email.Email;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PushService extends CoreService
{
    private static String START_SERVICE = "com.android.email.service.PushService.startService";
    private static String STOP_SERVICE = "com.android.email.service.PushService.stopService";

    public static void startService(Context context)
    {
        Intent i = new Intent();
        i.setClass(context, PushService.class);
        i.setAction(PushService.START_SERVICE);
        context.startService(i);
    }

    public static void stopService(Context context)
    {
        Intent i = new Intent();
        i.setClass(context, PushService.class);
        i.setAction(PushService.STOP_SERVICE);
        context.startService(i);
    }

    @Override
    public void startService(Intent intent, int startId)
    {
        if (START_SERVICE.equals(intent.getAction()))
        {
            Log.i(Email.LOG_TAG, "PushService started with startId = " + startId);
        }
        else if (STOP_SERVICE.equals(intent.getAction()))
        {
            Log.i(Email.LOG_TAG, "PushService stopping with startId = " + startId);
            stopSelf(startId);
        }

    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
