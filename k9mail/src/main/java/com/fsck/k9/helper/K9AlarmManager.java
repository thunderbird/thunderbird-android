package com.fsck.k9.helper;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;


public class K9AlarmManager {
    private final AlarmManager alarmManager;
    private final PowerManager powerManager;
    private final String packageName;


    @VisibleForTesting
    K9AlarmManager(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        packageName = context.getPackageName();
    }

    public static K9AlarmManager getAlarmManager(Context context) {
        return new K9AlarmManager(context);
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation) {
        if (isDozeSupported() && isDozeWhiteListed()) {
            setAndAllowWhileIdle(type, triggerAtMillis, operation);
        } else {
            alarmManager.set(type, triggerAtMillis, operation);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
        alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation);
    }

    public void cancel(PendingIntent operation) {
        alarmManager.cancel(operation);
    }

    @VisibleForTesting
    protected boolean isDozeSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isDozeWhiteListed() {
        return powerManager.isIgnoringBatteryOptimizations(packageName);
    }
}

