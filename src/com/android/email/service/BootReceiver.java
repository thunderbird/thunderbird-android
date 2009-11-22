
package com.android.email.service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.android.email.Email;

public class BootReceiver extends BroadcastReceiver {
    
    public static String WAKE_LOCK_RELEASE = "com.android.email.service.BroadcastReceiver.wakeLockRelease";
    public static String FIRE_INTENT = "com.android.email.service.BroadcastReceiver.fireIntent";
    public static String SCHEDULE_INTENT = "com.android.email.service.BroadcastReceiver.scheduleIntent";
    public static String CANCEL_INTENT = "com.android.email.service.BroadcastReceiver.cancelIntent";

    public static String WAKE_LOCK_ID = "com.android.email.service.BroadcastReceiver.wakeLockId";
    public static String ALARMED_INTENT = "com.android.email.service.BroadcastReceiver.pendingIntent";
    public static String AT_TIME = "com.android.email.service.BroadcastReceiver.atTime";
    
    private static ConcurrentHashMap<Integer, WakeLock> wakeLocks = new ConcurrentHashMap<Integer, WakeLock>();
    private static AtomicInteger wakeLockSeq = new AtomicInteger(0);
    
    private Integer getWakeLock(Context context)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(Email.BOOT_RECEIVER_WAKE_LOCK_TIMEOUT);
        Integer tmpWakeLockId = wakeLockSeq.getAndIncrement();
        wakeLocks.put(tmpWakeLockId, wakeLock);
        return tmpWakeLockId;
    }
    
    private void releaseWakeLock(Integer wakeLockId)
    {
        if (wakeLockId != null)
        {
            WakeLock wl = wakeLocks.remove(wakeLockId);
            if (wl != null)
            {
                wl.release();
            }
            else
            {
                Log.w(Email.LOG_TAG, "BootReceiver WakeLock " + wakeLockId + " doesn't exist");
            }
        }
    }
    
    public void onReceive(Context context, Intent intent) {
        Integer tmpWakeLockId = getWakeLock(context);
        try
        {
         	Log.i(Email.LOG_TAG, "BootReceiver.onReceive" + intent);
    
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
             	Email.setServicesEnabled(context, tmpWakeLockId);
             	tmpWakeLockId = null;
            }
            else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(intent.getAction())) {
                MailService.actionCancel(context, tmpWakeLockId);
                tmpWakeLockId = null;
            }
            else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(intent.getAction())) {
                MailService.actionReschedule(context, tmpWakeLockId);
                tmpWakeLockId = null;
            }
            else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                MailService.connectivityChange(context, !noConnectivity, tmpWakeLockId);
                tmpWakeLockId = null;
            }
            else if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(intent.getAction())) {
                MailService.backgroundDataChanged(context, tmpWakeLockId);
                tmpWakeLockId = null;
            }
            else if (FIRE_INTENT.equals(intent.getAction()))
            {
                Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
                String alarmedAction = alarmedIntent.getAction();
    
                Log.i(Email.LOG_TAG, "BootReceiver Got alarm to fire alarmedIntent " + alarmedAction);
                alarmedIntent.putExtra(WAKE_LOCK_ID, tmpWakeLockId);
                tmpWakeLockId = null;
                if (alarmedIntent != null)
                {
                    context.startService(alarmedIntent);
                }
            }
            else if (SCHEDULE_INTENT.equals(intent.getAction()))
            {
                long atTime = intent.getLongExtra(AT_TIME, -1);
                Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
                Log.i(Email.LOG_TAG,"BootReceiver Scheduling intent " + alarmedIntent + " for " + new Date(atTime));
                
                PendingIntent pi = buildPendingIntent(context, intent);
                AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                
                alarmMgr.set(AlarmManager.RTC_WAKEUP, atTime, pi);   
            }
            else if (CANCEL_INTENT.equals(intent.getAction()))
            {
                Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
                Log.i(Email.LOG_TAG, "BootReceiver Canceling alarmedIntent " + alarmedIntent);
                
                PendingIntent pi = buildPendingIntent(context, intent);
    
                AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                alarmMgr.cancel(pi);   
            }
            else if (BootReceiver.WAKE_LOCK_RELEASE.equals(intent.getAction()))
            {
                Integer wakeLockId = intent.getIntExtra(WAKE_LOCK_ID, -1);
                if (wakeLockId != -1)
                {
                    Log.i(Email.LOG_TAG, "BootReceiver Release wakeLock " + wakeLockId);
                    releaseWakeLock(wakeLockId);
                }
            }
        }
        finally
        {
            releaseWakeLock(tmpWakeLockId);
        }
    }
    
    private PendingIntent buildPendingIntent(Context context, Intent intent)
    {
        Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
        String alarmedAction = alarmedIntent.getAction();

        Intent i = new Intent(context, BootReceiver.class);
        i.setAction(FIRE_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        Uri uri = Uri.parse("action://" + alarmedAction);
        i.setData(uri);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        return pi;
    }
    
    public static void scheduleIntent(Context context, long atTime, Intent alarmedIntent)
    {
        Log.i(Email.LOG_TAG, "BootReceiver Got request to schedule alarmedIntent " + alarmedIntent.getAction());
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(SCHEDULE_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        i.putExtra(AT_TIME, atTime);
        context.sendBroadcast(i);
    }
    
    public static void cancelIntent(Context context, Intent alarmedIntent)
    {
        Log.i(Email.LOG_TAG, "BootReceiver Got request to cancel alarmedIntent " + alarmedIntent.getAction());
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(CANCEL_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        context.sendBroadcast(i);
    }
    
    public static void releaseWakeLock(Context context, int wakeLockId)
    {
        Log.i(Email.LOG_TAG, "BootReceiver Got request to release wakeLock " + wakeLockId);
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(WAKE_LOCK_RELEASE);
        i.putExtra(WAKE_LOCK_ID, wakeLockId);
        context.sendBroadcast(i);
    }
}
