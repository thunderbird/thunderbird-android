package com.fsck.k9.mail.store.imap

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
import com.fsck.k9.mail.store.imap.mockserver.MockImapServer
import com.fsck.k9.mail.testing.security.FakeTrustManager
import com.fsck.k9.mail.testing.security.SimpleTrustedSocketFactory
import java.net.UnknownHostException
import kotlin.test.Test

private const val USERNAME = "user"
private const val PASSWORD = "password"
private const val AUTHORIZATION_STATE = "auth state"
private const val AUTHORIZATION_TOKEN = "auth-token"
private val CLIENT_CERTIFICATE_ALIAS: String? = null
private const val CLIENT_NAME = "clientName"
private const val CLIENT_VERSION = "clientVersion"

class ImapServerSettingsValidatorTest {
    private val fakeTrustManager = FakeTrustManager()
    private val trustedSocketFactory = SimpleTrustedSocketFactory(fakeTrustManager)
    private val serverSettingsValidator = ImapServerSettingsValidator(
        trustedSocketFactory = trustedSocketFactory,
        oAuth2TokenProviderFactory = null,
        clientInfoAppName = CLIENT_NAME,
        clientInfoAppVersion = CLIENT_VERSION,
    )

    @Test
    fun `valid server settings with password should return Success`() {
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1 AUTH=PLAIN")
            output("1 OK CAPABILITY Completed")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("AHVzZXIAcGFzc3dvcmQ=")
            output("2 OK [CAPABILITY IMAP4rev1 AUTH=PLAIN NAMESPACE ID] LOGIN completed")
            expect("3 ID (\"name\" \"$CLIENT_NAME\" \"version\" \"$CLIENT_VERSION\")")
            output("* ID NIL")
            output("3 OK ID completed")
            expect("4 NAMESPACE")
            output("* NAMESPACE ((\"\" \"/\")) NIL NIL")
            output("4 OK command completed")
        }
        val serverSettings = ServerSettings(
            type = "imap",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
            extra = ImapStoreSettings.createExtra(
                autoDetectNamespace = true,
                pathPrefix = null,
                useCompression = false,
                sendClientInfo = true,
            ),
        )

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage = null)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `valid server settings with OAuth should return Success`() {
        val serverSettingsValidator = ImapServerSettingsValidator(
            trustedSocketFactory = trustedSocketFactory,
            oAuth2TokenProviderFactory = { authStateStorage ->
                assertThat(authStateStorage.getAuthorizationState()).isEqualTo(AUTHORIZATION_STATE)
                FakeOAuth2TokenProvider()
            },
            clientInfoAppName = CLIENT_NAME,
            clientInfoAppVersion = CLIENT_VERSION,
        )
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=OAUTHBEARER")
            output("1 OK CAPABILITY Completed")
            expect("2 AUTHENTICATE OAUTHBEARER bixhPXVzZXIsAWF1dGg9QmVhcmVyIGF1dGgtdG9rZW4BAQ==")
            output("2 OK [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=OAUTHBEARER NAMESPACE ID] LOGIN completed")
            expect("3 ID (\"name\" \"$CLIENT_NAME\" \"version\" \"$CLIENT_VERSION\")")
            output("* ID NIL")
            output("3 OK ID completed")
            expect("4 NAMESPACE")
            output("* NAMESPACE ((\"\" \"/\")) NIL NIL")
            output("4 OK command completed")
        }
        val serverSettings = ServerSettings(
            type = "imap",
            host = server.host,
            port = server.port,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.XOAUTH2,
            username = USERNAME,
            password = PASSWORD,
            clientCertificateAlias = CLIENT_CERTIFICATE_ALIAS,
            extra = ImapStoreSettings.createExtra(
                autoDetectNamespace = true,
                pathPrefix = null,
                useCompression = false,
                sendClientInfo = true,
            ),
        )
        val authStateStorage = FakeAuthStateStorage(authorizationState = AUTHORIZATION_STATE)

        val result = serverSettingsValidator.checkServerSettings(serverSettings, authStateStorage)

        assertThat(result).isInstanceOf<ServerSettingsValidationResult.Success>()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `authentication error should return AuthenticationError`() {
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1")
            output("1 OK CAPABILITY Completed")
            expect("2 LOGIN \"user\" \"password\"")
            output("2 NO [AUTHENTICATIONFAILED] Authentication failed")
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `error response should return ServerError`() {
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("1 BAD Something went wrong")
            closeConnection()
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `certificate error when trying to connect should return CertificateError`() {
        fakeTrustManager.shouldThrowException = true
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1 AUTH=PLAIN STARTTLS")
            output("1 OK CAPABILITY Completed")
            expect("2 STARTTLS")
            output("2 OK Begin TLS negotiation now")
            startTls()
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `missing capability should return MissingServerCapabilityError`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.RetrievalFailure)
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1")
            output("1 OK CAPABILITY Completed")
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `client certificate retrieval failure connect should return ClientCertificateRetrievalFailure`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.RetrievalFailure)
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1 AUTH=PLAIN STARTTLS")
            output("1 OK CAPABILITY Completed")
            expect("2 STARTTLS")
            output("2 OK Begin TLS negotiation now")
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `client certificate expired should return ClientCertificateExpired`() {
        trustedSocketFactory.injectClientCertificateError(ClientCertificateError.CertificateExpired)
        val server = startServer {
            output("* OK IMAP4rev1 server ready")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4rev1 AUTH=PLAIN STARTTLS")
            output("1 OK CAPABILITY Completed")
            expect("2 STARTTLS")
            output("2 OK Begin TLS negotiation now")
            startTls()
        }
        val serverSettings = ServerSettings(
            type = "imap",
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
    fun `non-existent hostname should return NetworkError`() {
        val serverSettings = ServerSettings(
            type = "imap",
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

    private fun startServer(block: MockImapServer.() -> Unit): MockImapServer {
        return MockImapServer().apply {
            block()
            start()
        }
    }
}

class FakeOAuth2TokenProvider(override val primaryEmail: String? = null) : OAuth2TokenProvider {
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
