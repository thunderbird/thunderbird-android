package com.fsck.k9.preferences

import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_DESCRIPTION_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_EMAIL_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_NAME_KEY

internal class IdentitySettingsWriter(private val generalSettingsManager: GeneralSettingsManager) {
    fun write(editor: StorageEditor, accountUuid: String, index: Int, identity: ValidatedSettings.Identity) {
        editor.putStringWithLogging(
            "$accountUuid.$IDENTITY_NAME_KEY.$index",
            identity.name,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )
        editor.putStringWithLogging(
            "$accountUuid.$IDENTITY_EMAIL_KEY.$index",
            identity.email,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )
        editor.putStringWithLogging(
            "$accountUuid.$IDENTITY_DESCRIPTION_KEY.$index",
            identity.description,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )

        // Convert identity settings to the representation used in preference storage
        val stringSettings = IdentitySettingsDescriptions.convert(identity.settings)

        for ((identityKey, value) in stringSettings) {
            val key = "$accountUuid.$identityKey.$index"
            editor.putStringWithLogging(
                key,
                value,
                generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
                generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
            )
        }
    }
}
