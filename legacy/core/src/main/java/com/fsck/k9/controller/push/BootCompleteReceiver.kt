package com.fsck.k9.controller.push

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class BootCompleteReceiver : BroadcastReceiver(), KoinComponent {
    private val pushController: PushController by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.v("BootCompleteReceiver.onReceive() - %s", intent?.action)

        pushController.init()
    }
}

class BootCompleteManager(context: Context) {
    private val packageManager = context.packageManager
    private val componentName = ComponentName(context, BootCompleteReceiver::class.java)

    fun enableReceiver() {
        Timber.v("Enable BootCompleteReceiver")
        try {
            packageManager.setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
        } catch (e: Exception) {
            Timber.e(e, "Error enabling BootCompleteReceiver")
        }
    }

    fun disableReceiver() {
        Timber.v("Disable BootCompleteReceiver")
        try {
            packageManager.setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
        } catch (e: Exception) {
            Timber.e(e, "Error disabling BootCompleteReceiver")
        }
    }
}
