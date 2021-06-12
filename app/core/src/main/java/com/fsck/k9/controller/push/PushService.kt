package com.fsck.k9.controller.push

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.fsck.k9.notification.PushNotificationManager
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Foreground service that is used to keep the app alive while listening for new emails (Push).
 */
class PushService : Service() {
    private val pushNotificationManager: PushNotificationManager by inject()
    private val pushController: PushController by inject()

    override fun onCreate() {
        Timber.v("PushService.onCreate()")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("PushService.onStartCommand()")
        super.onStartCommand(intent, flags, startId)

        startForeground()
        initializePushController()

        return START_STICKY
    }

    override fun onDestroy() {
        Timber.v("PushService.onDestroy()")
        pushNotificationManager.setForegroundServiceStopped()
        super.onDestroy()
    }

    private fun startForeground() {
        val notificationId = pushNotificationManager.notificationId
        val notification = pushNotificationManager.createForegroundNotification()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun initializePushController() {
        // When the app is killed by the system and later recreated to start this service nobody else is initializing
        // PushController. So we'll have to do it here.
        pushController.init()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
