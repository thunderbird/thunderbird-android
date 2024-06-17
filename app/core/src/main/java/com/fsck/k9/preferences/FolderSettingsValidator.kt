package com.fsck.k9.preferences

internal class FolderSettingsValidator {
    fun validate(contentVersion: Int, folder: SettingsFile.Folder): ValidatedSettings.Folder {
        val settings = folder.settings!!
        val folderName = folder.name!!

        val validatedSettings = FolderSettingsDescriptions.validate(contentVersion, settings, true)

        return ValidatedSettings.Folder(
            name = folderName,
            settings = validatedSettings,
        )
    }
}
