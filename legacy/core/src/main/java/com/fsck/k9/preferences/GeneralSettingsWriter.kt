package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler

internal class GeneralSettingsWriter(
    private val preferences: Preferences,
    private val generalSettingsManager: DefaultGeneralSettingsManager,
) {
    fun write(settings: InternalSettingsMap): Boolean {
        // Convert general settings to the string representation used in preference storage
        val stringSettings = GeneralSettingsDescriptions.convert(settings)

        val editor = preferences.createStorageEditor()

        // Use current general settings as base and overwrite with validated settings read from the import file.
        val mergedSettings = GeneralSettingsDescriptions.getGlobalSettings(preferences.storage).toMutableMap()
        mergedSettings.putAll(stringSettings)

        for ((key, value) in mergedSettings) {
            editor.putStringWithLogging(key, value, generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled)
        }

        return if (editor.commit()) {
            Log.v("Committed general settings to the preference storage.")

            generalSettingsManager.loadSettings()

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
internal fun StorageEditor.putStringWithLogging(key: String, value: String?, isDebugLoggingEnabled: Boolean) {
    if (isDebugLoggingEnabled) {
        var outputValue = value
        if (!K9.isSensitiveDebugLoggingEnabled &&
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
