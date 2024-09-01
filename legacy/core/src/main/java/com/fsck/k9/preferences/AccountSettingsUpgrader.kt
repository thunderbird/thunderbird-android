package com.fsck.k9.preferences

internal class AccountSettingsUpgrader(
    private val identitySettingsUpgrader: IdentitySettingsUpgrader,
    private val folderSettingsUpgrader: FolderSettingsUpgrader,
    private val serverSettingsUpgrader: ServerSettingsUpgrader,
) {

    fun upgrade(contentVersion: Int, account: ValidatedSettings.Account): ValidatedSettings.Account {
        if (contentVersion == Settings.VERSION) {
            return account
        }

        return account.copy(
            settings = AccountSettingsDescriptions.upgrade(contentVersion, account.settings),
            incoming = serverSettingsUpgrader.upgrade(contentVersion, account.incoming),
            outgoing = serverSettingsUpgrader.upgrade(contentVersion, account.outgoing),
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
