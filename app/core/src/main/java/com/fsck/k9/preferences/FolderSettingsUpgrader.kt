package com.fsck.k9.preferences

internal class FolderSettingsUpgrader {
    fun upgrade(contentVersion: Int, folder: ValidatedSettings.Folder): ValidatedSettings.Folder {
        val settings = folder.settings.toMutableMap()
        if (contentVersion != Settings.VERSION) {
            FolderSettingsDescriptions.upgrade(contentVersion, settings)
        }

        return folder.copy(settings = settings.toMap())
    }
}
