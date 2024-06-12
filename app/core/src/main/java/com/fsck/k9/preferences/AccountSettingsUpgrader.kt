package com.fsck.k9.preferences

internal class AccountSettingsUpgrader {
    fun upgrade(contentVersion: Int, account: ValidatedSettings.Account): ValidatedSettings.Account {
        val validatedSettings = account.settings.toMutableMap()
        if (contentVersion != Settings.VERSION) {
            AccountSettingsDescriptions.upgrade(contentVersion, validatedSettings)
        }

        return account.copy(settings = validatedSettings.toMap())
    }
}
