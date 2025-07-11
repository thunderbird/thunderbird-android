package com.fsck.k9.controller.push

import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import net.thunderbird.core.logging.legacy.Log

/**
 * Manages starting and stopping [PushService].
 */
internal class PushServiceManager(private val context: Context) {
    private var isServiceStarted = AtomicBoolean(false)

    fun start() {
        Log.v("PushServiceManager.start()")
        if (isServiceStarted.compareAndSet(false, true)) {
            startService()
        } else {
            Log.v("..PushService already running")
        }
    }

    fun stop() {
        Log.v("PushServiceManager.stop()")
        if (isServiceStarted.compareAndSet(true, false)) {
            stopService()
        } else {
            Log.v("..PushService is not running")
        }
    }

    fun setServiceStarted() {
        Log.v("PushServiceManager.setServiceStarted()")
        isServiceStarted.set(true)
    }

    fun setServiceStopped() {
        Log.v("PushServiceManager.setServiceStopped()")
        isServiceStarted.set(false)
    }

    private fun startService() {
        try {
            val intent = Intent(context, PushService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(e, "Exception while trying to start PushService")
        }
    }

    private fun stopService() {
        try {
            val intent = Intent(context, PushService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.w(e, "Exception while trying to stop PushService")
        }
    }
}
