package com.fsck.k9.preferences

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import kotlin.test.Test

class ServerSettingsValidatorTest {
    private val validator = ServerSettingsValidator()

    @Test
    fun `valid server settings`() {
        val result = validator.validate(contentVersion = 1, SERVER)

        assertThat(result).isEqualTo(VALIDATED_SERVER)
    }

    @Test
    fun `valid server settings with password`() {
        val server = SERVER.copy(password = "password")

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("password" to "password")),
        )
    }

    @Test
    fun `valid server settings with client certificate alias`() {
        val server = SERVER.copy(clientCertificateAlias = "alias")

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("clientCertificateAlias" to "alias")),
        )
    }

    @Test
    fun `valid server settings with arbitrary extras`() {
        val extras = mapOf("extra1" to "value1", "extra2" to "value2")
        val server = SERVER.copy(extras = extras)

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(VALIDATED_SERVER.copy(extras = extras))
    }

    // TODO: We currently allow the host value to be missing, but probably shouldn't
    @Test
    fun `missing host value`() {
        val server = SERVER.copy(host = null)

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("host" to null)),
        )
    }

    // TODO: We currently fall back to a default value of -1, but probably shouldn't
    @Test
    fun `missing port value`() {
        val server = SERVER.copy(port = null)

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("port" to -1)),
        )
    }

    // TODO: We currently fall back to a default value of -1, but probably shouldn't
    @Test
    fun `invalid port value`() {
        val server = SERVER.copy(port = "invalid")

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("port" to -1)),
        )
    }

    // TODO: We currently fall back to a default value of SSL_TLS_REQUIRED, but probably shouldn't
    @Test
    fun `missing connection security value`() {
        val server = SERVER.copy(connectionSecurity = null)

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("connectionSecurity" to "SSL_TLS_REQUIRED")),
        )
    }

    // TODO: We currently fall back to a default value of SSL_TLS_REQUIRED, but probably shouldn't
    @Test
    fun `invalid connection security value`() {
        val server = SERVER.copy(connectionSecurity = "invalid")

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("connectionSecurity" to "SSL_TLS_REQUIRED")),
        )
    }

    @Test
    fun `missing authentication type value`() {
        val server = SERVER.copy(authenticationType = null)

        assertFailure {
            validator.validate(contentVersion = 1, server)
        }.isInstanceOf<InvalidSettingValueException>()
            .hasMessage("Missing 'authenticationType' value")
    }

    @Test
    fun `invalid authentication type value`() {
        val server = SERVER.copy(authenticationType = "invalid")

        assertFailure {
            validator.validate(contentVersion = 1, server)
        }.isInstanceOf<InvalidSettingValueException>()
            .hasMessage("Missing 'authenticationType' value")
    }

    @Test
    fun `missing username value`() {
        val server = SERVER.copy(username = null)

        val result = validator.validate(contentVersion = 1, server)

        assertThat(result).isEqualTo(
            VALIDATED_SERVER.copy(settings = VALIDATED_SERVER.settings + ("username" to "")),
        )
    }

    companion object {
        private val SERVER = SettingsFile.Server(
            type = "IMAP",
            host = "imap.domain.example",
            port = "993",
            connectionSecurity = "SSL_TLS_REQUIRED",
            authenticationType = "PLAIN",
            username = "user",
            password = null,
            clientCertificateAlias = null,
            extras = mapOf(
                "autoDetectNamespace" to "true",
                "PATH_PREFIX_KEY" to "",
            ),
        )

        private val VALIDATED_SERVER = ValidatedSettings.Server(
            type = "imap",
            settings = mapOf(
                "host" to "imap.domain.example",
                "port" to 993,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "PLAIN",
                "username" to "user",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
            extras = mapOf(
                "autoDetectNamespace" to "true",
                "PATH_PREFIX_KEY" to "",
            ),
        )
    }
}
