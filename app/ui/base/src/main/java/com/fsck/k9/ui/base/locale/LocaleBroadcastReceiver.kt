package com.fsck.k9.ui.base.locale

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocaleBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    private val systemLocaleManager: SystemLocaleManager by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            systemLocaleManager.notifyListeners()
        }
    }
}
