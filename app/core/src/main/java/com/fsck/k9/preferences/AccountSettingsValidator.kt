package com.fsck.k9.preferences

import com.fsck.k9.preferences.ServerTypeConverter.toServerSettingsType
import com.fsck.k9.preferences.Settings.InvalidSettingValueException

internal class AccountSettingsValidator {
    fun validate(contentVersion: Int, account: SettingsFile.Account): ValidatedSettings.Account {
        val validatedSettings = AccountSettingsDescriptions.validate(contentVersion, account.settings!!, true)

        val incomingServer = validateIncomingServer(account.incoming)
        val outgoingServer = validateOutgoingServer(account.outgoing)

        return ValidatedSettings.Account(
            uuid = account.uuid,
            name = account.name,
            incoming = incomingServer,
            outgoing = outgoingServer,
            settings = validatedSettings,
        )
    }

    private fun validateIncomingServer(incoming: SettingsFile.Server?): ValidatedSettings.Server {
        if (incoming == null) {
            throw InvalidSettingValueException("Missing incoming server settings")
        }

        return validateServerSettings(incoming)
    }

    private fun validateOutgoingServer(outgoing: SettingsFile.Server?): ValidatedSettings.Server {
        if (outgoing == null) {
            throw InvalidSettingValueException("Missing outgoing server settings")
        }

        return validateServerSettings(outgoing)
    }

    private fun validateServerSettings(server: SettingsFile.Server): ValidatedSettings.Server {
        return ValidatedSettings.Server(
            type = toServerSettingsType(server.type!!),
            host = server.host,
            port = server.port?.toIntOrNull() ?: -1,
            connectionSecurity = server.connectionSecurity!!,
            authenticationType = server.authenticationType!!,
            username = server.username!!,
            password = server.password,
            clientCertificateAlias = server.clientCertificateAlias,
            extras = server.extras.orEmpty(),
        )
    }
}
