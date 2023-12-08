package com.fsck.k9.mail.store.pop3

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ClientCertificateError
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.helpers.FakeTrustManager
import com.fsck.k9.mail.helpers.SimpleTrustedSocketFactory
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import java.net.UnknownHostException
import kotlin.test.Test

private const val USERNAME = "user"
private const val PASSWORD = "password"
private val CLIENT_CERTIFICATE_ALIAS: String? = null

class Pop3ServerSettingsValidatorTest {
    private val fakeTrustManager = FakeTrustManager()
    private val trustedSocketFactory = SimpleTrustedSocketFactory(fakeTrustManager)
    private val serverSettingsValidator = Pop3ServerSettingsValidator(trustedSocketFactory)

    @Test
    fun `valid server settings should return Success`() {
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("STLS")
            output("UIDL")
            output("SASL PLAIN")
            output(".")
            expect("AUTH PLAIN")
            output("+OK")
            expect("AHVzZXIAcGFzc3dvcmQ=")
            output("+OK")
            expect("STAT")
            output("+OK 2 320")
            expect("QUIT")
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `authentication error should return AuthenticationError`() {
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("STLS")
            output("UIDL")
            output("SASL PLAIN")
            output(".")
            expect("AUTH PLAIN")
            output("+OK")
            expect("AHVzZXIAcGFzc3dvcmQ=")
            output("-ERR Authentication failed")
            expect("QUIT")
            closeConnection()
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.AuthenticationError>()
            .prop(ServerSettingsValidationResult.AuthenticationError::serverMessage)
            .isEqualTo("-ERR Authentication failed")
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `error code instead of greeting should return ServerError`() {
        val server = startServer {
            output("-ERR Service currently not available")
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.ServerError>()
            .prop(ServerSettingsValidationResult.ServerError::serverMessage)
            .isEqualTo("-ERR Service currently not available")
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `missing capability should return MissingServerCapabilityError`() {
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output(".")
            expect("QUIT")
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.MissingServerCapabilityError>()
            .prop(ServerSettingsValidationResult.MissingServerCapabilityError::capabilityName).isEqualTo("STLS")
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `client certificate retrieval failure should return ClientCertificateRetrievalFailure`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.RetrievalFailure)
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("STLS")
            output(".")
            expect("STLS")
            output("+OK Begin TLS negotiation")
            startTls()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result)
            .isInstanceOf<ServerSettingsValidationResult.ClientCertificateError.ClientCertificateRetrievalFailure>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `client certificate expired error should return ClientCertificateExpired`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.CertificateExpired)
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("STLS")
            output(".")
            expect("STLS")
            output("+OK Begin TLS negotiation")
            startTls()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result)
            .isInstanceOf<ServerSettingsValidationResult.ClientCertificateError.ClientCertificateExpired>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `certificate error when trying to connect should return CertificateError`() {
        fakeTrustManager.shouldThrowException = true
        val server = startServer {
            output("+OK POP3 server greeting")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("STLS")
            output("UIDL")
            output("SASL PLAIN")
            output(".")
            expect("STLS")
            output("+OK Begin TLS negotiation")
            startTls()
        }
        val serverSettings = ServerSettings(
            type = "pop3",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.CertificateError>()
            .prop(ServerSettingsValidationResult.CertificateError::certificateChain).hasSize(1)
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `non-existent hostname should return NetworkError`() {
        val serverSettings = ServerSettings(
            type = "pop3",
            host = "domain.invalid",
            port = 587,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

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
            serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)
        }.isInstanceOf<IllegalArgumentException>()
    }

    private fun startServer(block: MockPop3Server.() -> Unit): MockPop3Server {
        return MockPop3Server().apply {
            block()
            start()
        }
    }
}
