package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.common.mail.Protocols
import org.junit.Test

class SyncOptionsStateMapperKtTest {

    @Test
    fun `native API accounts default to ten messages per folder`() {
        listOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API).forEach { protocol ->
            val result = AccountState(incomingServerSettings = serverSettings(protocol))
                .toSyncOptionsState()

            assertThat(result.messageDisplayCount).isEqualTo(EmailDisplayCount.MESSAGES_10)
        }
    }

    @Test
    fun `IMAP and POP accounts keep the one hundred message default`() {
        listOf(Protocols.IMAP, Protocols.POP3).forEach { protocol ->
            val result = AccountState(incomingServerSettings = serverSettings(protocol))
                .toSyncOptionsState()

            assertThat(result.messageDisplayCount).isEqualTo(EmailDisplayCount.MESSAGES_100)
        }
    }

    @Test
    fun `Graph account keeps an explicitly selected display count`() {
        val options = AccountSyncOptions(
            checkFrequencyInMinutes = 60,
            messageDisplayCount = 50,
            showNotification = true,
        )
        listOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API).forEach { protocol ->
            val result = AccountState(
                incomingServerSettings = serverSettings(protocol),
                syncOptions = options,
            ).toSyncOptionsState()

            assertThat(result.messageDisplayCount).isEqualTo(EmailDisplayCount.MESSAGES_50)
        }
    }

    @Test
    fun `should map state to account options`() {
        val state = SyncOptionsContract.State(
            checkFrequency = EmailCheckFrequency.EVERY_2_HOURS,
            messageDisplayCount = EmailDisplayCount.MESSAGES_100,
            showNotification = true,
        )

        val result = state.toAccountSyncOptions()

        assertThat(result).isEqualTo(
            AccountSyncOptions(
                checkFrequencyInMinutes = 120,
                messageDisplayCount = 100,
                showNotification = true,
            ),
        )
    }

    private fun serverSettings(protocol: String) = ServerSettings(
        type = protocol,
        host = if (protocol == Protocols.GRAPH) "graph.microsoft.com" else "$protocol.example.com",
        port = if (protocol in setOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API)) 443 else 993,
        connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = if (protocol in setOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API)) {
            AuthType.XOAUTH2
        } else {
            AuthType.PLAIN
        },
        username = "user@example.com",
        password = if (protocol in setOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API)) {
            null
        } else {
            "password"
        },
        clientCertificateAlias = null,
    )
}
