package com.fsck.k9.preferences

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerSettingsUpgraderTest {
    private val upgrader = ServerSettingsUpgrader()

    @Test
    fun `upgrade from version 1 to 92`() {
        val server = ValidatedSettings.Server(
            type = "SMTP",
            settings = mapOf(
                "host" to "smtp.domain.example",
                "port" to 465,
                "connectionSecurity" to "SSL_TLS_OPTIONAL",
                "authenticationType" to "PLAIN",
                "username" to "user",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
            extras = emptyMap(),
        )

        val upgradedServer = upgrader.upgrade(targetVersion = 92, contentVersion = 1, server)

        assertThat(upgradedServer).isEqualTo(
            server.copy(
                settings = server.settings + ("connectionSecurity" to "SSL_TLS_REQUIRED"),
            ),
        )
    }
}
