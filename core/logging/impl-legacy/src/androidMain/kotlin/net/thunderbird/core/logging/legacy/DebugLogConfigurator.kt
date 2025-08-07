package net.thunderbird.core.logging.legacy

import timber.log.Timber

// TODO: Implementation https://github.com/thunderbird/thunderbird-android/issues/9573
class DebugLogConfigurator {

    fun updateLoggingStatus(isDebugLoggingEnabled: Boolean) {
        Timber.uprootAll()
        if (isDebugLoggingEnabled) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
