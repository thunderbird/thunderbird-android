package com.fsck.k9.preferences

import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.CLIENT_CERTIFICATE_ALIAS
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.CONNECTION_SECURITY
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.HOST
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.PORT
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.USERNAME
import com.fsck.k9.preferences.ServerTypeConverter.toServerSettingsType
import com.fsck.k9.preferences.Settings.InvalidSettingValueException

internal class ServerSettingsValidator(
    private val serverSettingsDescriptions: ServerSettingsDescriptions = ServerSettingsDescriptions(),
) {
    fun validate(contentVersion: Int, server: SettingsFile.Server): ValidatedSettings.Server {
        val settings = convertServerSettingsToMap(server)

        val validatedSettings = Settings.validate(contentVersion, serverSettingsDescriptions.settings, settings, true)

        if (validatedSettings[AUTHENTICATION_TYPE] !is String) {
            throw InvalidSettingValueException("Missing '$AUTHENTICATION_TYPE' value")
        }

        return ValidatedSettings.Server(
            type = toServerSettingsType(server.type!!),
            settings = validatedSettings,
            extras = server.extras.orEmpty(),
        )
    }

    private fun convertServerSettingsToMap(server: SettingsFile.Server): SettingsMap {
        return buildMap {
            server.host?.let { host -> put(HOST, host) }
            server.port?.let { port -> put(PORT, port) }
            server.connectionSecurity?.let { connectionSecurity -> put(CONNECTION_SECURITY, connectionSecurity) }
            server.authenticationType?.let { authenticationType -> put(AUTHENTICATION_TYPE, authenticationType) }
            server.username?.let { username -> put(USERNAME, username) }
            server.password?.let { password -> put(PASSWORD, password) }
            server.clientCertificateAlias?.let { clientCertificateAlias ->
                put(CLIENT_CERTIFICATE_ALIAS, clientCertificateAlias)
            }
        }
    }
}
