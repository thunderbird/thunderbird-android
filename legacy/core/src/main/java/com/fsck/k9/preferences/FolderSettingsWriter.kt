package com.fsck.k9.preferences

import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.StorageEditor

internal class FolderSettingsWriter(private val generalSettingsManager: GeneralSettingsManager) {
    fun write(editor: StorageEditor, accountUuid: String, folder: ValidatedSettings.Folder) {
        // Convert folder settings to the string representation used in preference storage
        val stringSettings = FolderSettingsDescriptions.convert(folder.settings)

        // Write folder settings
        val prefix = "$accountUuid.${folder.name}."
        for ((folderKey, value) in stringSettings) {
            val key = prefix + folderKey
            editor.putStringWithLogging(
                key,
                value,
                generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
                generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
            )
        }
    }
}
