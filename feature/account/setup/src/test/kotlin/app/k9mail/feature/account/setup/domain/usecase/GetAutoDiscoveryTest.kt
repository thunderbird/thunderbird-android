package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetAutoDiscoveryTest {

    @Test
    fun `should return a valid result`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(SETTINGS_WITH_PASSWORD),
            oauthProvider = FakeOAuthConfigurationProvider(OAUTH_CONFIGURATION),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.Settings>()
            .isEqualTo(SETTINGS_WITH_PASSWORD)
    }

    @Test
    fun `should return NoUsableSettingsFound result`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(AutoDiscoveryResult.NoUsableSettingsFound),
            oauthProvider = FakeOAuthConfigurationProvider(),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.NoUsableSettingsFound>()
    }

    @Test
    fun `should return NoUsableSettingsFound result when incoming server settings not supported`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(SETTINGS_WITH_UNSUPPORTED_INCOMING_SERVER),
            oauthProvider = FakeOAuthConfigurationProvider(),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.NoUsableSettingsFound>()
    }

    @Test
    fun `should return NoUsableSettingsFound result when server outgoing settings not supported`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(SETTINGS_WITH_UNSUPPORTED_OUTGOING_SERVER),
            oauthProvider = FakeOAuthConfigurationProvider(),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.NoUsableSettingsFound>()
    }

    @Test
    fun `should return UnexpectedException result`() = runTest {
        val autoDiscoveryResult = AutoDiscoveryResult.UnexpectedException(Exception("unexpected exception"))
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(autoDiscoveryResult),
            oauthProvider = FakeOAuthConfigurationProvider(),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.UnexpectedException>()
            .isEqualTo(autoDiscoveryResult)
    }

    @Test
    fun `should check for oauth support and return when supported`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(SETTINGS_WITH_OAUTH),
            oauthProvider = FakeOAuthConfigurationProvider(OAUTH_CONFIGURATION),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.Settings>()
            .isEqualTo(SETTINGS_WITH_OAUTH)
    }

    @Test
    fun `should check for OAuth support and drop OAuth when not supported`() = runTest {
        val useCase = GetAutoDiscovery(
            service = FakeAutoDiscoveryService(SETTINGS_WITH_OAUTH),
            oauthProvider = FakeOAuthConfigurationProvider(),
        )

        val result = useCase.execute("user@example.com")

        assertThat(result)
            .isInstanceOf<AutoDiscoveryResult.Settings>()
            .isEqualTo(
                SETTINGS_WITH_OAUTH.copy(
                    incomingServerSettings = (SETTINGS_WITH_OAUTH.incomingServerSettings as ImapServerSettings).copy(
                        authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
                    ),
                    outgoingServerSettings = (SETTINGS_WITH_OAUTH.outgoingServerSettings as SmtpServerSettings).copy(
                        authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
                    ),
                ),
            )
    }

    private class FakeAutoDiscoveryService(
        private val answer: AutoDiscoveryResult = AutoDiscoveryResult.NoUsableSettingsFound,
    ) : AutoDiscoveryService {
        override suspend fun discover(email: EmailAddress): AutoDiscoveryResult = answer
    }

    private class FakeOAuthConfigurationProvider(
        private val answer: OAuthConfiguration? = null,
    ) : OAuthConfigurationProvider {
        override fun getConfiguration(hostname: String): OAuthConfiguration? = answer
    }

    private class UnsupportedIncomingServerSettings : IncomingServerSettings
    private class UnsupportedOutgoingServerSettings : OutgoingServerSettings

    private companion object {
        private val SETTINGS_WITH_OAUTH = AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.example.com".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.OAuth2, AuthenticationType.PasswordCleartext),
                username = "user",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.example.com".toHostname(),
                port = 465.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.OAuth2, AuthenticationType.PasswordCleartext),
                username = "user",
            ),
            isTrusted = true,
            source = "source",
        )

        private val SETTINGS_WITH_UNSUPPORTED_INCOMING_SERVER = AutoDiscoveryResult.Settings(
            incomingServerSettings = UnsupportedIncomingServerSettings(),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.example.com".toHostname(),
                port = 465.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.OAuth2),
                username = "user",
            ),
            isTrusted = true,
            source = "source",
        )

        private val SETTINGS_WITH_UNSUPPORTED_OUTGOING_SERVER = AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.example.com".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.OAuth2, AuthenticationType.PasswordCleartext),
                username = "user",
            ),
            outgoingServerSettings = UnsupportedOutgoingServerSettings(),
            isTrusted = true,
            source = "source",
        )

        private val SETTINGS_WITH_PASSWORD = AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.example.com".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
                username = "user",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.example.com".toHostname(),
                port = 465.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
                username = "user",
            ),
            isTrusted = true,
            source = "source",
        )

        private val OAUTH_CONFIGURATION = OAuthConfiguration(
            clientId = "clientId",
            scopes = listOf("scopes"),
            authorizationEndpoint = "authorizationEndpoint",
            tokenEndpoint = "tokenEndpoint",
            redirectUri = "redirectUri",
        )
    }
}
