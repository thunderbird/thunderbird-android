package com.fsck.k9.preferences

internal class AccountSettingsUpgrader(
    private val identitySettingsUpgrader: IdentitySettingsUpgrader,
    private val folderSettingsUpgrader: FolderSettingsUpgrader,
    private val serverSettingsUpgrader: ServerSettingsUpgrader,
    private val latestVersion: Int = Settings.VERSION,
    private val settingsDescriptions: SettingsDescriptions = AccountSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = AccountSettingsDescriptions.UPGRADERS,
) {

    fun upgrade(contentVersion: Int, account: ValidatedSettings.Account): ValidatedSettings.Account {
        if (contentVersion == latestVersion) {
            return account
        }

        return account.copy(
            settings = upgradeAccountSettings(contentVersion, account.settings),
            incoming = serverSettingsUpgrader.upgrade(contentVersion, account.incoming),
            outgoing = serverSettingsUpgrader.upgrade(contentVersion, account.outgoing),
            identities = upgradeIdentities(contentVersion, account.identities),
            folders = upgradeFolders(contentVersion, account.folders),
        )
    }

    private fun upgradeAccountSettings(
        contentVersion: Int,
        settings: InternalSettingsMap,
    ): InternalSettingsMap {
        return SettingsUpgradeHelper.upgrade(contentVersion, upgraders, settingsDescriptions, settings)
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
