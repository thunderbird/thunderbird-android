package com.fsck.k9.controller.push

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.fsck.k9.notification.PushNotificationManager
import net.thunderbird.core.logging.legacy.Log
import org.koin.android.ext.android.inject

/**
 * Foreground service that is used to keep the app alive while listening for new emails (Push).
 */
class PushService : Service() {
    private val pushServiceManager: PushServiceManager by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val pushController: PushController by inject()

    override fun onCreate() {
        Log.v("PushService.onCreate()")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("PushService.onStartCommand(%s)", intent)
        super.onStartCommand(intent, flags, startId)

        val isAutomaticRestart = intent == null
        if (isAutomaticRestart) {
            maybeStartForeground()
            initializePushController()
        } else {
            startForeground()
        }

        notifyServiceStarted()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.v("PushService.onDestroy()")
        pushNotificationManager.setForegroundServiceStopped()
        notifyServiceStopped()
        super.onDestroy()
    }

    private fun maybeStartForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            startForeground()
        } else {
            try {
                startForeground()
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.e(e, "Ignoring ForegroundServiceStartNotAllowedException during automatic restart.")

                // This works around what seems to be a bug in at least Android 14.
                // See https://github.com/thunderbird/thunderbird-android/issues/7416 for more details.
            }
        }
    }

    private fun startForeground() {
        val notificationId = pushNotificationManager.notificationId
        val notification = pushNotificationManager.createForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun notifyServiceStarted() {
        // If our process was low-memory killed and now this service is being restarted by the system,
        // PushServiceManager doesn't necessarily know about this service's state. So we're updating it now.
        pushServiceManager.setServiceStarted()
    }

    private fun notifyServiceStopped() {
        // Usually this service is only stopped via PushServiceManager. But we still notify PushServiceManager here in
        // case the system decided to stop the service (without killing the process).
        pushServiceManager.setServiceStopped()
    }

    private fun initializePushController() {
        // When the app is killed by the system and later recreated to start this service nobody else is initializing
        // PushController. So we'll have to do it here.
        pushController.init()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
