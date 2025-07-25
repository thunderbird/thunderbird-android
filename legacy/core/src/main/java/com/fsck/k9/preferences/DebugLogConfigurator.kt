package com.fsck.k9.preferences

import timber.log.Timber
import timber.log.Timber.DebugTree

class DebugLogConfigurator {

    fun updateLoggingStatus(isDebugLoggingEnabled: Boolean) {
        Timber.uprootAll()
        if (isDebugLoggingEnabled) {
            Timber.plant(DebugTree())
        }
    }
}
