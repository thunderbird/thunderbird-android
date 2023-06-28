package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.setup.domain.entity.AuthenticationType
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class AccountIncomingConfigStateMapperKtTest {

    @Test
    fun `should map to server settings`() {
        val incomingState = State(
            protocolType = IncomingProtocolType.IMAP,
            server = StringInputField(value = "imap.example.org"),
            port = NumberInputField(value = 993),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificate = "",
            imapAutodetectNamespaceEnabled = true,
            imapPrefix = StringInputField(value = "prefix"),
            imapUseCompression = true,
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
            ),
        )
    }
}
