package com.fsck.k9.preferences

import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings

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
        val connectionSecurity = convertConnectionSecurity(server.connectionSecurity)
        val authenticationType = AuthType.valueOf(server.authenticationType)
        val password = if (authenticationType == AuthType.XOAUTH2) "" else server.password

        return ServerSettings(
            server.type,
            server.host,
            server.port,
            connectionSecurity,
            authenticationType,
            server.username,
            password,
            server.clientCertificateAlias,
            server.extras,
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun convertConnectionSecurity(connectionSecurity: String): ConnectionSecurity {
        return try {
            // TODO: Add proper settings validation and upgrade capability for server settings. Once that exists, move
            //  this code into a SettingsUpgrader.
            when (connectionSecurity) {
                "SSL_TLS_OPTIONAL" -> ConnectionSecurity.SSL_TLS_REQUIRED
                "STARTTLS_OPTIONAL" -> ConnectionSecurity.STARTTLS_REQUIRED
                else -> ConnectionSecurity.valueOf(connectionSecurity)
            }
        } catch (e: Exception) {
            ConnectionSecurity.SSL_TLS_REQUIRED
        }
    }
}
