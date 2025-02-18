package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import org.junit.Test

class IncomingServerSettingsStateMapperKtTest {

    @Test
    fun `should map to state with email as username and emailDomain With dot prefix as server name when server settings are null`() {
        val email = "test@example.com"
        val accountState = AccountState(
            emailAddress = "test@example.com",
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
    }
}
