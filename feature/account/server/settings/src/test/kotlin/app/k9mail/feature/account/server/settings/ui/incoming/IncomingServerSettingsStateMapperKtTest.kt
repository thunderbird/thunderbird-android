package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class IncomingServerSettingsStateMapperKtTest {

    @Suppress("MaxLineLength")
    @Test
    fun `should map to state with email as username and emailDomain With dot prefix as server name when server settings are null`() {
        val email = "test@example.com"
        val accountState = AccountState(
            emailAddress = email,
            incomingServerSettings = null,
        )

        val result = accountState.toIncomingServerSettingsState()

        assertThat(result).isEqualTo(
            State(
                username = StringInputField(value = email),
                server = StringInputField(value = email.toInvalidEmailDomain()),
            ),
        )
    }

    @Test
    fun `should map to default state when incoming server settings use an unsupported protocol`() {
        val email = "test@example.com"
        val accountState = AccountState(
            emailAddress = email,
            incomingServerSettings = DEMO_SERVER_SETTINGS,
        )

        val result = accountState.toIncomingServerSettingsState()

        assertThat(result).isEqualTo(
            State(
                username = StringInputField(value = email),
                server = StringInputField(value = email.toInvalidEmailDomain()),
            ),
        )
    }

    @Test
    fun `should map from IMAP server settings to state`() {
        val serverSettings = AccountState(
            incomingServerSettings = IMAP_SERVER_SETTINGS,
        )

        val result = serverSettings.toIncomingServerSettingsState()

        assertThat(result).isEqualTo(INCOMING_IMAP_STATE)
    }

    @Test
    fun `should map from state to IMAP server settings and trim`() {
        val incomingState = INCOMING_IMAP_STATE.copy(
            server = StringInputField(value = " imap.example.org "),
            username = StringInputField(value = " user "),
            password = StringInputField(value = " password "),
            imapPrefix = StringInputField(value = " prefix "),
        )

        val result = incomingState.toServerSettings()

        assertThat(result).isEqualTo(IMAP_SERVER_SETTINGS)
    }

    @Test
    fun `should map from POP3 server settings to state`() {
        val serverSettings = AccountState(
            incomingServerSettings = POP3_SERVER_SETTINGS,
        )

        val result = serverSettings.toIncomingServerSettingsState()

        assertThat(result).isEqualTo(INCOMING_POP3_STATE)
    }

    @Test
    fun `should map from state to POP3 server settings and trim`() {
        val incomingState = INCOMING_POP3_STATE.copy(
            server = StringInputField(value = " pop3.example.org "),
            username = StringInputField(value = " user "),
            password = StringInputField(value = " password "),
        )

        val result = incomingState.toServerSettings()

        assertThat(result).isEqualTo(POP3_SERVER_SETTINGS)
    }

    @Test
    fun `should map from Graph server settings to OAuth state`() {
        val result = AccountState(
            incomingServerSettings = ServerSettings(
                type = "graph",
                host = "graph.microsoft.com",
                port = 443,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.XOAUTH2,
                username = "user@example.com",
                password = null,
                clientCertificateAlias = null,
            ),
        ).toIncomingServerSettingsState()

        assertThat(result.protocolType).isEqualTo(IncomingProtocolType.GRAPH)
        assertThat(result.server.value).isEqualTo("graph.microsoft.com")
        assertThat(result.port.value).isEqualTo(443L)
        assertThat(result.authenticationType).isEqualTo(AuthenticationType.OAuth2)
        assertThat(result.username.value).isEqualTo("user@example.com")
        assertThat(result.preserveLegacyGraphProtocol).isEqualTo(true)
    }

    @Test
    fun `should map Graph state to OAuth server settings`() {
        val result = State(
            protocolType = IncomingProtocolType.GRAPH,
            server = StringInputField(value = " graph.microsoft.com "),
            security = ConnectionSecurity.TLS,
            port = NumberInputField(value = 443),
            authenticationType = AuthenticationType.OAuth2,
            username = StringInputField(value = " user@example.com "),
            preserveLegacyGraphProtocol = true,
        ).toServerSettings()

        assertThat(result.type).isEqualTo("graph")
        assertThat(result.host).isEqualTo("graph.microsoft.com")
        assertThat(result.port).isEqualTo(443)
        assertThat(result.authenticationType).isEqualTo(AuthType.XOAUTH2)
        assertThat(result.username).isEqualTo("user@example.com")
        assertThat(result.password).isEqualTo(null)
    }

    @Test
    fun `should preserve per-account Client ID and Redirect URI through a Graph settings edit`() {
        val redirectUri = "net.thunderbird.android.debug://oauth2redirect"
        val serverSettings = ServerSettings(
            type = "msgraphapi",
            host = "graph.microsoft.com",
            port = 443,
            connectionSecurity = com.fsck.k9.mail.ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = "user@example.com",
            password = null,
            clientCertificateAlias = null,
            extra = mapOf(
                NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID to "account-specific-client",
                NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI to redirectUri,
            ),
        )

        val result = AccountState(incomingServerSettings = serverSettings)
            .toIncomingServerSettingsState()
            .toServerSettings()

        assertThat(result.extra).isEqualTo(serverSettings.extra)
    }

    @Test
    fun `should use the canonical msgraphapi identifier for a new Graph account`() {
        val result = State(
            protocolType = IncomingProtocolType.GRAPH,
            server = StringInputField(value = "graph.microsoft.com"),
            port = NumberInputField(value = 443),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.OAuth2,
        ).toServerSettings()

        assertThat(result.type).isEqualTo("msgraphapi")
    }

    private companion object {
        private val INCOMING_IMAP_STATE = State(
            protocolType = IncomingProtocolType.IMAP,
            server = StringInputField(value = "imap.example.org"),
            port = NumberInputField(value = 993),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificateAlias = null,
            imapAutodetectNamespaceEnabled = false,
            imapPrefix = StringInputField(value = "prefix"),
            imapUseCompression = true,
            imapSendClientInfo = true,
        )

        private val IMAP_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 993,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
            extra = ImapStoreSettings.createExtra(
                autoDetectNamespace = false,
                pathPrefix = "prefix",
                useCompression = true,
                sendClientInfo = true,
            ),
        )

        private val INCOMING_POP3_STATE = State(
            protocolType = IncomingProtocolType.POP3,
            server = StringInputField(value = "pop3.example.org"),
            port = NumberInputField(value = 995),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificateAlias = null,
        )

        private val POP3_SERVER_SETTINGS = ServerSettings(
            type = "pop3",
            host = "pop3.example.org",
            port = 995,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        private val DEMO_SERVER_SETTINGS = ServerSettings(
            type = "demo",
            host = "irrelevant",
            port = 23,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "irrelevant",
            password = "irrelevant",
            clientCertificateAlias = null,
        )
    }
}
