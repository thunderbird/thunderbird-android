@file:Suppress("DEPRECATION")

package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.SettingsChangeListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

internal class RealGeneralSettingsManager(
    private val preferences: Preferences
) : GeneralSettingsManager {
    override fun getSettings(): GeneralSettings {
        return createGeneralSettings()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSettingsFlow(): Flow<GeneralSettings> {
        return callbackFlow {
            send(createGeneralSettings())

            val listener = SettingsChangeListener {
                try {
                    sendBlocking(createGeneralSettings())
                } catch (e: Exception) {
                    Timber.w(e, "Error while trying to send to channel")
                }
            }
            preferences.addSettingsChangeListener(listener)

            awaitClose {
                preferences.removeSettingsChangeListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
    }

    private fun createGeneralSettings(): GeneralSettings {
        return GeneralSettings(
            backgroundSync = K9.backgroundOps.toBackgroundSync()
        )
    }
}

private fun K9.BACKGROUND_OPS.toBackgroundSync(): BackgroundSync {
    return when (this) {
        K9.BACKGROUND_OPS.ALWAYS -> BackgroundSync.ALWAYS
        K9.BACKGROUND_OPS.NEVER -> BackgroundSync.NEVER
        K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC -> BackgroundSync.FOLLOW_SYSTEM_AUTO_SYNC
    }
}
