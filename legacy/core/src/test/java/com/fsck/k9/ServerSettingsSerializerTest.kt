package com.fsck.k9

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import org.junit.Test

class ServerSettingsSerializerTest {
    private val serverSettingsSerializer = ServerSettingsSerializer()

    @Test
    fun `serialize and deserialize IMAP server settings`() {
        val serverSettings = ServerSettings(
            type = "imap",
            host = "imap.domain.example",
            port = 143,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = null,
            clientCertificateAlias = "alias",
            extra = ImapStoreSettings.createExtra(autoDetectNamespace = true, pathPrefix = null),
        )

        val json = serverSettingsSerializer.serialize(serverSettings)
        val deserializedServerSettings = serverSettingsSerializer.deserialize(json)

        assertThat(deserializedServerSettings).isEqualTo(serverSettings)
    }

    @Test
    fun `serialize and deserialize POP3 server settings`() {
        val serverSettings = ServerSettings(
            type = "pop3",
            host = "pop3.domain.example",
            port = 995,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        val json = serverSettingsSerializer.serialize(serverSettings)
        val deserializedServerSettings = serverSettingsSerializer.deserialize(json)

        assertThat(deserializedServerSettings).isEqualTo(serverSettings)
    }

    @Test
    fun `deserialize JSON with missing type`() {
        val json = """
            {
                "host": "imap.domain.example",
                "port": 993,
                "connectionSecurity": "SSL_TLS_REQUIRED",
                "authenticationType": "PLAIN",
                "username": "user",
                "password": "pass",
                "clientCertificateAlias": null
            }
        """.trimIndent()

        assertFailure {
            serverSettingsSerializer.deserialize(json)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("'type' must not be missing")
    }
}
