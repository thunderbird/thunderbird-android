package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.InvalidSettingValueException

internal class IdentitySettingsValidator {
    fun validate(contentVersion: Int, identity: SettingsFile.Identity): ValidatedSettings.Identity {
        if (!IdentitySettingsDescriptions.isEmailAddressValid(identity.email)) {
            throw InvalidSettingValueException("Invalid email address: " + identity.email)
        }

        val validatedSettings = IdentitySettingsDescriptions.validate(
            contentVersion,
            identity.settings.orEmpty(),
            true,
        )

        return ValidatedSettings.Identity(
            name = identity.name.orEmpty(),
            email = identity.email!!,
            description = identity.description,
            settings = validatedSettings,
        )
    }
}
