package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.InvalidSettingValueException

internal class AccountSettingsValidator {
    private val identitySettingsValidator = IdentitySettingsValidator()
    private val folderSettingsValidator = FolderSettingsValidator()
    private val serverSettingsValidator = ServerSettingsValidator()

    fun validate(contentVersion: Int, account: SettingsFile.Account): ValidatedSettings.Account {
        val validatedSettings = AccountSettingsDescriptions.validate(contentVersion, account.settings!!, true)

        val incomingServer = validateIncomingServer(contentVersion, account.incoming)
        val outgoingServer = validateOutgoingServer(contentVersion, account.outgoing)

        return ValidatedSettings.Account(
            uuid = account.uuid,
            name = account.name,
            incoming = incomingServer,
            outgoing = outgoingServer,
            settings = validatedSettings,
            identities = validateIdentities(contentVersion, account.identities),
            folders = validateFolders(contentVersion, account.folders),
        )
    }

    private fun validateIdentities(
        contentVersion: Int,
        identities: List<SettingsFile.Identity>?,
    ): List<ValidatedSettings.Identity> {
        if (identities.isNullOrEmpty()) {
            throw InvalidSettingValueException("Missing identities, there should be at least one.")
        }

        return identities.map { identity ->
            identitySettingsValidator.validate(contentVersion, identity)
        }
    }

    private fun validateFolders(
        contentVersion: Int,
        folders: List<SettingsFile.Folder>?,
    ): List<ValidatedSettings.Folder> {
        return folders.orEmpty()
            .map { folder ->
                folderSettingsValidator.validate(contentVersion, folder)
            }
    }

    private fun validateIncomingServer(contentVersion: Int, incoming: SettingsFile.Server?): ValidatedSettings.Server {
        if (incoming == null) {
            throw InvalidSettingValueException("Missing incoming server settings")
        }

        return serverSettingsValidator.validate(contentVersion, incoming)
    }

    private fun validateOutgoingServer(contentVersion: Int, outgoing: SettingsFile.Server?): ValidatedSettings.Server {
        if (outgoing == null) {
            throw InvalidSettingValueException("Missing outgoing server settings")
        }

        return serverSettingsValidator.validate(contentVersion, outgoing)
    }
}
