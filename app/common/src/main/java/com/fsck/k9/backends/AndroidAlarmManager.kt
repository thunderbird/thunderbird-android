package com.fsck.k9.backends

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.fsck.k9.backend.imap.SystemAlarmManager
import com.fsck.k9.helper.AlarmManagerCompat
import com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

private const val ALARM_ACTION = "com.fsck.k9.backends.ALARM"
private const val REQUEST_CODE = 1

private typealias Callback = () -> Unit

class AndroidAlarmManager(
    private val context: Context,
    private val alarmManager: AlarmManagerCompat,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SystemAlarmManager {
    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val pendingIntent: PendingIntent = run {
        val intent = Intent(ALARM_ACTION).apply {
            setPackage(context.packageName)
        }

        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, FLAG_IMMUTABLE)
    }

    private val callback = AtomicReference<Callback?>(null)

    init {
        val intentFilter = IntentFilter(ALARM_ACTION)
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val callback = callback.getAndSet(null)
                    if (callback == null) {
                        Timber.w("Alarm triggered but 'callback' was null")
                    } else {
                        coroutineScope.launch {
                            callback.invoke()
                        }
                    }
                }
            },
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun setAlarm(triggerTime: Long, callback: Callback) {
        this.callback.set(callback)
        alarmManager.scheduleAlarm(triggerTime, pendingIntent)
    }

    override fun cancelAlarm() {
        callback.set(null)
        alarmManager.cancelAlarm(pendingIntent)
    }

    override fun now(): Long = SystemClock.elapsedRealtime()
}
