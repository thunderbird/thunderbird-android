package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import kotlin.test.assertFailsWith
import net.thunderbird.core.common.net.Hostname
import net.thunderbird.core.common.net.Port
import org.junit.Test

class AutoDiscoveryMapperKtTest {

    @Test
    fun `should map IncomingServerSettings to ServerSettings`() {
        val incomingServerSettings = ImapServerSettings(
            hostname = Hostname("imap.example.org"),
            port = Port(993),
            connectionSecurity = ConnectionSecurity.TLS,
            authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
            username = "user",
        )
        val password = "password"

        val serverSettings = incomingServerSettings.toServerSettings(password)

        assertThat(serverSettings).isEqualTo(
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
                    sendClientInfo = true,
                ),
            ),
        )
    }

    @Test
    fun `should throw error when IncomingServerSettings not known`() {
        val incomingServerSettings = object : IncomingServerSettings {}

        assertFailsWith<IllegalArgumentException> {
            incomingServerSettings.toServerSettings("password")
        }
    }

    @Test
    fun `should map OutgoingServerSettings to ServerSettings`() {
        val outgoingServerSettings = SmtpServerSettings(
            hostname = Hostname("smtp.example.org"),
            port = Port(587),
            connectionSecurity = ConnectionSecurity.StartTLS,
            authenticationTypes = listOf(AuthenticationType.PasswordCleartext),
            username = "user",
        )
        val password = "password"

        val serverSettings = outgoingServerSettings.toServerSettings(password)

        assertThat(serverSettings).isEqualTo(
            ServerSettings(
                type = "smtp",
                host = "smtp.example.org",
                port = 587,
                connectionSecurity = MailConnectionSecurity.STARTTLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "password",
                clientCertificateAlias = null,
                extra = emptyMap(),
            ),
        )
    }

    @Test
    fun `should throw error when OutgoingServerSettings not known`() {
        val outgoingServerSettings = object : OutgoingServerSettings {}

        assertFailsWith<IllegalArgumentException> {
            outgoingServerSettings.toServerSettings("password")
        }
    }
}
