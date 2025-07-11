package com.fsck.k9.controller.push

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import net.thunderbird.core.logging.legacy.Log

/**
 * Starting with Android 12 we have to check whether the app can schedule exact alarms.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class AlarmPermissionManagerApi31(
    private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmPermissionManager {
    private var isRegistered = false
    private var listener: AlarmPermissionListener? = null

    private val intentFilter = IntentFilter().apply {
        addAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val listener = synchronized(this@AlarmPermissionManagerApi31) { listener }
            listener?.onAlarmPermissionGranted()
        }
    }

    override fun canScheduleExactAlarms(): Boolean {
        return AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
    }

    @Synchronized
    override fun registerListener(listener: AlarmPermissionListener) {
        if (!isRegistered) {
            Log.v("Registering alarm permission listener")
            isRegistered = true
            this.listener = listener
            ContextCompat.registerReceiver(context, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    @Synchronized
    override fun unregisterListener() {
        if (isRegistered) {
            Log.v("Unregistering alarm permission listener")
            isRegistered = false
            listener = null
            context.unregisterReceiver(receiver)
        }
    }
}
