package com.fsck.k9.mail.transport.smtp

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.helpers.FakeTrustManager
import com.fsck.k9.mail.helpers.SimpleTrustedSocketFactory
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.transport.mockServer.MockSmtpServer
import java.net.UnknownHostException
import kotlin.test.Test

private const val USERNAME = "user"
private const val PASSWORD = "password"
private val CLIENT_CERTIFICATE_ALIAS: String? = null

class SmtpServerSettingsValidatorTest {
    private val fakeTrustManager = FakeTrustManager()
    private val serverSettingsValidator = SmtpServerSettingsValidator(
        trustedSocketFactory = SimpleTrustedSocketFactory(fakeTrustManager),
        oAuth2TokenProvider = null,
    )

    @Test
    fun `valid server settings should return Success`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO [127.0.0.1]")
            output("250-localhost Hello client.localhost")
            output("250-ENHANCEDSTATUSCODES")
            output("250-AUTH PLAIN LOGIN")
            output("250 HELP")
            expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=")
            output("235 2.7.0 Authentication successful")
            expect("QUIT")
            closeConnection()
        }
        server.start()
        val serverSettings = ServerSettings(
            type = "smtp",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `authentication error should return AuthenticationError`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO [127.0.0.1]")
            output("250-localhost Hello client.localhost")
            output("250-ENHANCEDSTATUSCODES")
            output("250-AUTH PLAIN LOGIN")
            output("250 HELP")
            expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=")
            output("535 5.7.8 Authentication failed")
            expect("QUIT")
            closeConnection()
        }
        server.start()
        val serverSettings = ServerSettings(
            type = "smtp",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.AuthenticationError>()
            .prop(ServerSettingsValidationResult.AuthenticationError::serverMessage).isEqualTo("Authentication failed")
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `error code instead of greeting should return ServerError`() {
        val server = MockSmtpServer().apply {
            output("421 domain.example Service currently not available")
            closeConnection()
        }
        server.start()
        val serverSettings = ServerSettings(
            type = "smtp",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.ServerError>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `certificate error when trying to connect should return CertificateError`() {
        fakeTrustManager.shouldThrowException = true
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO [127.0.0.1]")
            output("250-localhost Hello 127.0.0.1")
            output("250-STARTTLS")
            output("250 HELP")
            expect("STARTTLS")
            output("220 Ready to start TLS")
            startTls()
        }
        server.start()
        val serverSettings = ServerSettings(
            type = "smtp",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.CertificateError>()
            .prop(ServerSettingsValidationResult.CertificateError::certificateChain).hasSize(1)
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `non-existent hostname should return NetworkError`() {
        val serverSettings = ServerSettings(
            type = "smtp",
            host = "domain.invalid",
            port = 587,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.NetworkError>()
            .prop(ServerSettingsValidationResult.NetworkError::exception)
            .isInstanceOf<UnknownHostException>()
    }

    @Test
    fun `ServerSettings with wrong type should throw`() {
        val serverSettings = ServerSettings(
            type = "wrong",
            host = "domain.invalid",
            port = 587,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        assertFailure {
            serverSettingsValidator.checkServerSettings(serverSettings)
        }.isInstanceOf<IllegalArgumentException>()
    }
}
