package com.fsck.k9.controller.push

import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import net.thunderbird.legacy.logging.Log

private const val TAG = "PushServiceManager"

/**
 * Manages starting and stopping [PushService].
 */
internal class PushServiceManager(private val context: Context) {
    private var isServiceStarted = AtomicBoolean(false)

    fun start() {
        Log.v(TAG, "PushServiceManager.start()")
        if (isServiceStarted.compareAndSet(false, true)) {
            startService()
        } else {
            Log.v(TAG, "..PushService already running")
        }
    }

    fun stop() {
        Log.v(TAG, "PushServiceManager.stop()")
        if (isServiceStarted.compareAndSet(true, false)) {
            stopService()
        } else {
            Log.v(TAG, "..PushService is not running")
        }
    }

    fun setServiceStarted() {
        Log.v(TAG, "PushServiceManager.setServiceStarted()")
        isServiceStarted.set(true)
    }

    fun setServiceStopped() {
        Log.v(TAG, "PushServiceManager.setServiceStopped()")
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
            Log.e(TAG, e, "Exception while trying to start PushService")
        }
    }

    private fun stopService() {
        try {
            val intent = Intent(context, PushService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.w(TAG, e, "Exception while trying to stop PushService")
        }
    }
}
