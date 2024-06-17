package com.fsck.k9.preferences

internal class AccountSettingsUpgrader {
    private val identitySettingsUpgrader = IdentitySettingsUpgrader()
    private val folderSettingsUpgrader = FolderSettingsUpgrader()

    fun upgrade(contentVersion: Int, account: ValidatedSettings.Account): ValidatedSettings.Account {
        val validatedSettings = account.settings.toMutableMap()
        if (contentVersion != Settings.VERSION) {
            AccountSettingsDescriptions.upgrade(contentVersion, validatedSettings)
        }

        return account.copy(
            settings = validatedSettings.toMap(),
            identities = upgradeIdentities(contentVersion, account.identities),
            folders = upgradeFolders(contentVersion, account.folders),
        )
    }

    private fun upgradeIdentities(
        contentVersion: Int,
        identities: List<ValidatedSettings.Identity>,
    ): List<ValidatedSettings.Identity> {
        return identities.map { identity ->
            identitySettingsUpgrader.upgrade(contentVersion, identity)
        }
    }

    private fun upgradeFolders(
        contentVersion: Int,
        folders: List<ValidatedSettings.Folder>,
    ): List<ValidatedSettings.Folder> {
        return folders.map { folder ->
            folderSettingsUpgrader.upgrade(contentVersion, folder)
        }
    }
}
