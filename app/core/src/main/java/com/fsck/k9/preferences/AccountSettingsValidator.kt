package com.fsck.k9.preferences

internal class AccountSettingsValidator {
    fun validate(contentVersion: Int, account: SettingsFile.Account): ValidatedSettings.Account {
        val validatedSettings = AccountSettingsDescriptions.validate(contentVersion, account.settings!!, true)

        return ValidatedSettings.Account(
            uuid = account.uuid,
            name = account.name,
            settings = validatedSettings,
        )
    }
}
