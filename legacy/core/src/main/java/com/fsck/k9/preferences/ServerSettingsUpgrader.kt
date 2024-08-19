package com.fsck.k9.preferences

internal class ServerSettingsUpgrader(
    private val serverSettingsDescriptions: ServerSettingsDescriptions = ServerSettingsDescriptions(),
) {
    fun upgrade(contentVersion: Int, server: ValidatedSettings.Server): ValidatedSettings.Server {
        if (contentVersion == Settings.VERSION) {
            return server
        }

        val upgradedSettings = Settings.upgrade(
            contentVersion,
            serverSettingsDescriptions.upgraders,
            serverSettingsDescriptions.settings,
            server.settings,
        )

        return server.copy(settings = upgradedSettings)
    }
}
