package com.fsck.k9.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

class AlarmManagerCompat(private val alarmManager: AlarmManager) {
    fun scheduleAlarm(triggerAtMillis: Long, operation: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, operation)
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, operation)
        }
    }

    fun cancelAlarm(operation: PendingIntent) {
        alarmManager.cancel(operation)
    }
}
