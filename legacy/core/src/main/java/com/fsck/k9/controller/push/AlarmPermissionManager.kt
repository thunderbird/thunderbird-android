package com.fsck.k9.controller.push

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Checks whether the app can schedule exact alarms.
 */
internal interface AlarmPermissionManager {
    /**
     * Checks whether the app can schedule exact alarms.
     *
     * If this method returns `false`, the app has to request the permission to schedule exact alarms. See
     * [Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM].
     */
    fun canScheduleExactAlarms(): Boolean

    /**
     * Register a listener to be notified when the app was granted the permission to schedule exact alarms.
     */
    fun registerListener(listener: AlarmPermissionListener)

    /**
     * Unregister the listener registered via [registerListener].
     */
    fun unregisterListener()
}

/**
 * Factory method to create an Android API-specific instance of [AlarmPermissionManager].
 */
internal fun AlarmPermissionManager(context: Context, alarmManager: AlarmManager): AlarmPermissionManager {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlarmPermissionManagerApi31(context, alarmManager)
    } else {
        AlarmPermissionManagerApi21()
    }
}

/**
 * Listener that can be notified when the app was granted the permission to schedule exact alarms.
 *
 * Note: Currently Android stops (and potentially restarts) the app when the permission is revoked. So there's no
 * callback mechanism for the permission revocation case.
 */
internal fun interface AlarmPermissionListener {
    fun onAlarmPermissionGranted()
}
