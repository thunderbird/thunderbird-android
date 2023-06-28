package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class AccountOutgoingConfigStateMapperKtTest {

    @Test
    fun `should map to server settings`() {
        val outgoingState = State(
            server = StringInputField(value = "smtp.example.org"),
            port = NumberInputField(value = 587),
            security = ConnectionSecurity.TLS,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificate = "",
        )

        val result = outgoingState.toServerSettings()

        assertThat(result).isEqualTo(
            ServerSettings(
                type = "smtp",
                host = "smtp.example.org",
                port = 587,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "password",
                clientCertificateAlias = null,
            ),
        )
    }
}
