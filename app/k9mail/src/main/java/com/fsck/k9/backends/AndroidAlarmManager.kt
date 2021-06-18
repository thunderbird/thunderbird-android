package com.fsck.k9.backends

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import com.fsck.k9.backend.imap.SystemAlarmManager
import com.fsck.k9.helper.AlarmManagerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ALARM_ACTION = "com.fsck.k9.backends.ALARM"
private const val REQUEST_CODE = 1

class AndroidAlarmManager(
    private val context: Context,
    private val alarmManager: AlarmManagerCompat,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SystemAlarmManager {
    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val pendingIntent: PendingIntent = run {
        val intent = Intent(ALARM_ACTION).apply {
            setPackage(context.packageName)
        }
        val flags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    @Volatile
    private var callback: (() -> Unit)? = null

    init {
        val intentFilter = IntentFilter(ALARM_ACTION)
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    coroutineScope.launch {
                        callback?.invoke()
                        callback = null
                    }
                }
            },
            intentFilter
        )
    }

    override fun setAlarm(triggerTime: Long, callback: () -> Unit) {
        this.callback = callback
        alarmManager.scheduleAlarm(triggerTime, pendingIntent)
    }

    override fun cancelAlarm() {
        alarmManager.cancelAlarm(pendingIntent)
    }

    override fun now(): Long = SystemClock.elapsedRealtime()
}
