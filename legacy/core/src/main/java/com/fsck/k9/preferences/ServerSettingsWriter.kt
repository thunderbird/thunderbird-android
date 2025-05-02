package com.fsck.k9.preferences

import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.preferences.ServerSettingsDescriptions.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.CLIENT_CERTIFICATE_ALIAS
import com.fsck.k9.preferences.ServerSettingsDescriptions.CONNECTION_SECURITY
import com.fsck.k9.preferences.ServerSettingsDescriptions.HOST
import com.fsck.k9.preferences.ServerSettingsDescriptions.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.PORT
import com.fsck.k9.preferences.ServerSettingsDescriptions.USERNAME

internal class ServerSettingsWriter(
    private val serverSettingsSerializer: ServerSettingsSerializer,
) {
    fun writeServerSettings(
        editor: StorageEditor,
        key: String,
        server: ValidatedSettings.Server,
    ) {
        val serverSettings = createServerSettings(server)
        val serverSettingsJson = serverSettingsSerializer.serialize(serverSettings)
        editor.putStringWithLogging(key, serverSettingsJson)
    }

    private fun createServerSettings(server: ValidatedSettings.Server): ServerSettings {
        val validatedSettings = server.settings

        val host = validatedSettings[HOST] as String
        val port = validatedSettings[PORT] as Int
        val connectionSecurity = ConnectionSecurity.valueOf(validatedSettings[CONNECTION_SECURITY] as String)
        val authenticationType = AuthType.valueOf(validatedSettings[AUTHENTICATION_TYPE] as String)
        val username = validatedSettings[USERNAME] as String
        val rawPassword = validatedSettings[PASSWORD] as? String
        val password = if (authenticationType == AuthType.XOAUTH2) "" else rawPassword
        val clientCertificateAlias = validatedSettings[CLIENT_CERTIFICATE_ALIAS] as? String

        return ServerSettings(
            server.type,
            host,
            port,
            connectionSecurity,
            authenticationType,
            username,
            password,
            clientCertificateAlias,
            server.extras,
        )
    }
}
