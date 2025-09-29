package com.fsck.k9.mail.transport.smtp

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
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.testing.security.FakeTrustManager
import com.fsck.k9.mail.testing.security.SimpleTrustedSocketFactory
import com.fsck.k9.mail.transport.mockServer.MockSmtpServer
import java.net.UnknownHostException
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import okio.ByteString.Companion.encodeUtf8
import org.junit.Before

private const val USERNAME = "user"
private const val PASSWORD = "password"
private const val AUTHORIZATION_STATE = "auth state"
private const val AUTHORIZATION_TOKEN = "auth-token"
private val CLIENT_CERTIFICATE_ALIAS: String? = null

class SmtpServerSettingsValidatorTest {
    private val fakeTrustManager = FakeTrustManager()
    private val trustedSocketFactory = SimpleTrustedSocketFactory(fakeTrustManager)
    private val serverSettingsValidator = SmtpServerSettingsValidator(
        trustedSocketFactory = trustedSocketFactory,
        oAuth2TokenProviderFactory = null,
    )

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `valid server settings with password should return Success`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `valid server settings with OAuth should return Success`() {
        val serverSettingsValidator = SmtpServerSettingsValidator(
            trustedSocketFactory = trustedSocketFactory,
            oAuth2TokenProviderFactory = { authStateStorage ->
                assertThat(authStateStorage.getAuthorizationState()).isEqualTo(AUTHORIZATION_STATE)
                FakeOAuth2TokenProvider()
            },
        )
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250-ENHANCEDSTATUSCODES")
            output("250-AUTH PLAIN LOGIN OAUTHBEARER")
            output("250 HELP")
            expect("AUTH OAUTHBEARER bixhPXVzZXIsAWF1dGg9QmVhcmVyIGF1dGgtdG9rZW4BAQ==")
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
            authenticationType = AuthType.XOAUTH2,
            username = USERNAME,
            password = null,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )
        val authStateStorage = FakeAuthStateStorage(authorizationState = AUTHORIZATION_STATE)

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `valid server settings with primary email different from username on OAuth should return Success`() {
        // Arrange
        val expectedUser = "expected@email.com"
        val serverSettingsValidator = SmtpServerSettingsValidator(
            trustedSocketFactory = trustedSocketFactory,
            oAuth2TokenProviderFactory = { authStateStorage ->
                assertThat(authStateStorage.getAuthorizationState()).isEqualTo(AUTHORIZATION_STATE)
                FakeOAuth2TokenProvider(usernames = setOf(expectedUser))
            },
        )

        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO $SMTP_HELLO_NAME")
            output("250-localhost Hello $SMTP_HELLO_NAME")
            output("250-ENHANCEDSTATUSCODES")
            output("250-AUTH PLAIN LOGIN XOAUTH2")
            output("250 HELP")

            var ouathBearer = "user=${USERNAME}\u0001auth=Bearer ${AUTHORIZATION_TOKEN}\u0001\u0001"
                .encodeUtf8()
                .base64()

            expect("AUTH XOAUTH2 $ouathBearer")
            output("535 5.7.3 Authentication unsuccessful")

            ouathBearer = "user=${expectedUser}\u0001auth=Bearer ${AUTHORIZATION_TOKEN}\u0001\u0001"
                .encodeUtf8()
                .base64()

            expect("AUTH XOAUTH2 $ouathBearer")
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
            authenticationType = AuthType.XOAUTH2,
            username = USERNAME,
            password = null,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
        )

        val authStateStorage = FakeAuthStateStorage(authorizationState = AUTHORIZATION_STATE)

        // Act
        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage)

        // Assert
        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `authentication error should return AuthenticationError`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.ServerError>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `missing capability should return MissingServerCapabilityError`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 HELP")
            expect("QUIT")
            closeConnection()
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.MissingServerCapabilityError>()
            .prop(ServerSettingsValidationResult.MissingServerCapabilityError::capabilityName).isEqualTo("STARTTLS")
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `client certificate retrieval failure should return ClientCertificateRetrievalFailure`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.RetrievalFailure)
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result)
            .isInstanceOf<ServerSettingsValidationResult.ClientCertificateError.ClientCertificateRetrievalFailure>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `client certificate expired error should return ClientCertificateExpired`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.CertificateExpired)
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result)
            .isInstanceOf<ServerSettingsValidationResult.ClientCertificateError.ClientCertificateExpired>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `certificate error when trying to connect should return CertificateError`() {
        fakeTrustManager.shouldThrowException = true
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
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

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

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
}

class FakeOAuth2TokenProvider(override val usernames: Set<String> = emptySet()) : OAuth2TokenProvider {
    override fun getToken(timeoutMillis: Long): String {
        return AUTHORIZATION_TOKEN
    }

    override fun invalidateToken() {
        throw UnsupportedOperationException("not implemented")
    }
}

class FakeAuthStateStorage(
    private var authorizationState: String? = null,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? {
        return authorizationState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        this.authorizationState = authorizationState
    }
}
