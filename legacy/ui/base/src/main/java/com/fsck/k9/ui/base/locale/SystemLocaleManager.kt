package com.fsck.k9.ui.base.locale

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.CopyOnWriteArraySet
import net.thunderbird.core.logging.legacy.Log

class SystemLocaleManager(context: Context) {
    private val packageManager = context.packageManager
    private val componentName = ComponentName(context, LocaleBroadcastReceiver::class.java)

    private val listeners = CopyOnWriteArraySet<SystemLocaleChangeListener>()

    @Synchronized
    fun addListener(listener: SystemLocaleChangeListener) {
        if (listeners.isEmpty()) {
            enableReceiver()
        }

        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: SystemLocaleChangeListener) {
        listeners.remove(listener)

        if (listeners.isEmpty()) {
            disableReceiver()
        }
    }

    internal fun notifyListeners() {
        for (listener in listeners) {
            listener.onSystemLocaleChanged()
        }
    }

    private fun enableReceiver() {
        Log.v("Enable LocaleBroadcastReceiver")
        try {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            Log.e(e, "Error enabling LocaleBroadcastReceiver")
        }
    }

    private fun disableReceiver() {
        Log.v("Disable LocaleBroadcastReceiver")
        try {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            Log.e(e, "Error disabling LocaleBroadcastReceiver")
        }
    }
}

fun interface SystemLocaleChangeListener {
    fun onSystemLocaleChanged()
}
