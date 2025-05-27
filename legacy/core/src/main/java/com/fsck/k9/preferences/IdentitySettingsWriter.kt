package com.fsck.k9.preferences

import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_DESCRIPTION_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_EMAIL_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.IDENTITY_NAME_KEY

internal class IdentitySettingsWriter {
    fun write(editor: StorageEditor, accountUuid: String, index: Int, identity: ValidatedSettings.Identity) {
        editor.putStringWithLogging("$accountUuid.$IDENTITY_NAME_KEY.$index", identity.name)
        editor.putStringWithLogging("$accountUuid.$IDENTITY_EMAIL_KEY.$index", identity.email)
        editor.putStringWithLogging("$accountUuid.$IDENTITY_DESCRIPTION_KEY.$index", identity.description)

        // Convert identity settings to the representation used in preference storage
        val stringSettings = IdentitySettingsDescriptions.convert(identity.settings)

        for ((identityKey, value) in stringSettings) {
            val key = "$accountUuid.$identityKey.$index"
            editor.putStringWithLogging(key, value)
        }
    }
}
