package com.fsck.k9.preferences

import net.thunderbird.core.preference.PreferenceChangePublisher
import net.thunderbird.core.preference.getPreferenceScope
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.legacy.logging.Log

internal class GeneralSettingsWriter(
    private val storageProvider: StorageProvider,
    private val generalSettingsManager: DefaultGeneralSettingsManager,
    private val changePublisher: PreferenceChangePublisher,
) {
    fun write(settings: InternalSettingsMap): Boolean {
        // Convert general settings to the string representation used in preference storage
        val stringSettings = GeneralSettingsDescriptions.convert(settings)

        val editor = storageProvider.createStorageEditor()

        // Use current general settings as base and overwrite with validated settings read from the import file.
        val mergedSettings = GeneralSettingsDescriptions.getGlobalSettings(storageProvider.storage).toMutableMap()
        mergedSettings.putAll(stringSettings)

        for ((key, value) in mergedSettings) {
            editor.putStringWithLogging(
                key,
                value,
                generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
                generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
            )
        }

        return if (editor.commit()) {
            Log.v("Committed general settings to the preference storage.")

            generalSettingsManager.loadSettings()
            mergedSettings.keys.forEach {
                val preferenceScope = getPreferenceScope(it)
                changePublisher.publish(scope = preferenceScope)
            }

            true
        } else {
            Log.v("Failed to commit general settings to the preference storage")
            false
        }
    }
}

/**
 * Write to a [StorageEditor] while logging what is written if debug logging is enabled.
 */
internal fun StorageEditor.putStringWithLogging(
    key: String,
    value: String?,
    isDebugLoggingEnabled: Boolean,
    isSensitiveDebugLoggingEnabled: Boolean,
) {
    if (isDebugLoggingEnabled) {
        var outputValue = value
        if (!isSensitiveDebugLoggingEnabled &&
            (
                key.endsWith("." + LegacyAccountStorageHandler.OUTGOING_SERVER_SETTINGS_KEY) ||
                    key.endsWith("." + LegacyAccountStorageHandler.INCOMING_SERVER_SETTINGS_KEY)
                )
        ) {
            outputValue = "*sensitive*"
        }

        Log.v("Setting %s=%s", key, outputValue)
    }

    putString(key, value)
}
