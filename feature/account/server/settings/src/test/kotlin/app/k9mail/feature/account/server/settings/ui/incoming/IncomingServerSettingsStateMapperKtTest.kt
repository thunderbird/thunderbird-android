package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import org.junit.Test

class IncomingServerSettingsStateMapperKtTest {

    @Test
    fun `should map to IMAP server settings`() {
        val incomingState = State(
            protocolType = IncomingProtocolType.IMAP,
            server = StringInputField(value = "imap.example.org"),
            port = NumberInputField(value = 993),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificateAlias = null,
            imapAutodetectNamespaceEnabled = true,
            imapPrefix = StringInputField(value = "prefix"),
            imapUseCompression = true,
            imapSendClientId = true,
        )

        val result = incomingState.toServerSettings()

        assertThat(result).isEqualTo(
            ServerSettings(
                type = "imap",
                host = "imap.example.org",
                port = 993,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "password",
                clientCertificateAlias = null,
                extra = ImapStoreSettings.createExtra(
                    autoDetectNamespace = true,
                    pathPrefix = null,
                    useCompression = true,
                    sendClientId = true,
                ),
            ),
        )
    }

    @Test
    fun `should map to POP3 server settings`() {
        val incomingState = State(
            protocolType = IncomingProtocolType.POP3,
            server = StringInputField(value = "pop3.domain.example"),
            port = NumberInputField(value = 995),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificateAlias = null,
            imapAutodetectNamespaceEnabled = true,
            imapPrefix = StringInputField(value = "prefix"),
            imapUseCompression = true,
            imapSendClientId = true,
        )

        val result = incomingState.toServerSettings()

        assertThat(result).isEqualTo(
            ServerSettings(
                type = "pop3",
                host = "pop3.domain.example",
                port = 995,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "password",
                clientCertificateAlias = null,
            ),
        )
    }
}
