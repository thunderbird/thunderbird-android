package com.fsck.k9.helper;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.power.DozeChecker;


public class K9AlarmManager {
    private final AlarmManager alarmManager;
    private final DozeChecker dozeChecker;


    public static K9AlarmManager getAlarmManager(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        DozeChecker dozeChecker = new DozeChecker(context);
        return new K9AlarmManager(alarmManager, dozeChecker);
    }

    @VisibleForTesting
    K9AlarmManager(AlarmManager alarmManager, DozeChecker dozeChecker) {
        this.alarmManager = alarmManager;
        this.dozeChecker = dozeChecker;
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation) {
        if (dozeChecker.isDeviceIdleModeSupported() && dozeChecker.isAppWhitelisted()) {
            setAndAllowWhileIdle(type, triggerAtMillis, operation);
        } else {
            alarmManager.set(type, triggerAtMillis, operation);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
        alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation);
    }

    public void cancel(PendingIntent operation) {
        alarmManager.cancel(operation);
    }
}

