package com.fsck.k9.preferences

import net.thunderbird.core.preference.GeneralSettingsManager

internal class AccountSettingsUpgrader(
    private val identitySettingsUpgrader: IdentitySettingsUpgrader,
    private val folderSettingsUpgrader: FolderSettingsUpgrader,
    private val serverSettingsUpgrader: ServerSettingsUpgrader,
    private val latestVersion: Int = Settings.VERSION,
    private val settingsDescriptions: SettingsDescriptions = AccountSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = AccountSettingsDescriptions.UPGRADERS,
    private val combinedUpgraders: Map<Int, CombinedSettingsUpgraderFactory> = CombinedSettingsUpgraders.UPGRADERS,
    private val generalSettingsManager: GeneralSettingsManager,
) {

    fun upgrade(contentVersion: Int, account: ValidatedSettings.Account): ValidatedSettings.Account {
        if (contentVersion == latestVersion) {
            return account
        }

        val relevantCombinedUpgraderVersions = combinedUpgraders.keys.asSequence()
            .filter { it > contentVersion }
            .toSortedSet()

        var currentAccount = account
        var currentVersion = contentVersion
        for (version in relevantCombinedUpgraderVersions) {
            val toVersion = version - 1
            currentAccount = upgradeToVersion(toVersion, currentVersion, currentAccount)

            val combinedUpgrader = combinedUpgraders[version]!!.invoke()
            currentAccount = combinedUpgrader.upgrade(currentAccount)

            currentAccount = upgradeToVersion(version, toVersion, currentAccount)
            currentVersion = version
        }

        return upgradeToVersion(toVersion = latestVersion, contentVersion = currentVersion, currentAccount)
    }

    private fun upgradeToVersion(
        toVersion: Int,
        contentVersion: Int,
        account: ValidatedSettings.Account,
    ): ValidatedSettings.Account {
        return account.copy(
            settings = upgradeAccountSettings(toVersion, contentVersion, account.settings),
            incoming = serverSettingsUpgrader.upgrade(toVersion, contentVersion, account.incoming),
            outgoing = serverSettingsUpgrader.upgrade(toVersion, contentVersion, account.outgoing),
            identities = upgradeIdentities(toVersion, contentVersion, account.identities),
            folders = upgradeFolders(toVersion, contentVersion, account.folders),
        )
    }

    private fun upgradeAccountSettings(
        toVersion: Int,
        contentVersion: Int,
        settings: InternalSettingsMap,
    ): InternalSettingsMap {
        return SettingsUpgradeHelper.upgradeToVersion(
            toVersion,
            contentVersion,
            upgraders,
            settingsDescriptions,
            settings,
            generalSettingsManager,
        )
    }

    private fun upgradeIdentities(
        toVersion: Int,
        contentVersion: Int,
        identities: List<ValidatedSettings.Identity>,
    ): List<ValidatedSettings.Identity> {
        return identities.map { identity ->
            identitySettingsUpgrader.upgrade(toVersion, contentVersion, identity)
        }
    }

    private fun upgradeFolders(
        toVersion: Int,
        contentVersion: Int,
        folders: List<ValidatedSettings.Folder>,
    ): List<ValidatedSettings.Folder> {
        return folders.map { folder ->
            folderSettingsUpgrader.upgrade(toVersion, contentVersion, folder)
        }
    }
}
