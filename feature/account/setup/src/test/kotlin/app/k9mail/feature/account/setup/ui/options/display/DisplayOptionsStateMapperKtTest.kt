package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class DisplayOptionsStateMapperKtTest {

    @Test
    fun `should map state to account options`() {
        val state = DisplayOptionsContract.State(
            accountName = StringInputField("accountName"),
            displayName = StringInputField("displayName"),
            emailSignature = StringInputField("emailSignature"),
        )

        val result = state.toAccountDisplayOptions()

        assertThat(result).isEqualTo(
            AccountDisplayOptions(
                accountName = "accountName",
                displayName = "displayName",
                emailSignature = "emailSignature",
            ),
        )
    }

    @Test
    fun `default account name is the email address for native and legacy protocols`() {
        val emailAddress = "user@example.test"
        val protocols = listOf(
            Protocols.GMAIL_API,
            Protocols.ZOHO_MAIL_API,
            Protocols.GRAPH,
            Protocols.GRAPH_LEGACY,
            Protocols.IMAP,
            Protocols.POP3,
        )

        protocols.forEach { protocol ->
            val accountState = AccountState(
                emailAddress = emailAddress,
                incomingServerSettings = ServerSettings(
                    type = protocol,
                    host = "mail.example.test",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.NONE,
                    username = "",
                    password = null,
                    clientCertificateAlias = null,
                ),
            )

            assertThat(accountState.toDisplayOptionsState().accountName.value).isEqualTo(emailAddress)
        }
    }

    @Test
    fun `existing account name is not overwritten`() {
        val accountState = AccountState(
            emailAddress = "user@example.test",
            displayOptions = AccountDisplayOptions(
                accountName = "Work mailbox",
                displayName = "Sender",
                emailSignature = null,
            ),
        )

        assertThat(accountState.toDisplayOptionsState().accountName.value).isEqualTo("Work mailbox")
    }

    @Test
    fun `empty signature should map to null`() {
        val state = DisplayOptionsContract.State(emailSignature = StringInputField(""))

        val result = state.toAccountDisplayOptions()

        assertThat(result.emailSignature).isNull()
    }
}
