package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class OutgoingServerSettingsStateMapperKtTest {

    @Test
    fun `should map to state with email as username when server settings are null`() {
        val accountState = AccountState(
            emailAddress = "test@example.com",
            outgoingServerSettings = null,
        )

        val result = accountState.toOutgoingServerSettingsState()

        assertThat(result).isEqualTo(
            State(
                username = StringInputField(value = "test@example.com"),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `should map to state with password from incomingServerSettings when outgoingServerSettings is null`() {
        val accountState = AccountState(
            emailAddress = "test@domain.example",
            incomingServerSettings = IMAP_SERVER_SETTINGS,
            outgoingServerSettings = null,
        )

        val result = accountState.toOutgoingServerSettingsState()

        assertThat(result).isEqualTo(
            State(
                username = StringInputField(value = "test@domain.example"),
                password = StringInputField(value = INCOMING_SERVER_PASSWORD),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `should map from SMTP server settings to state`() {
        val accountState = AccountState(
            outgoingServerSettings = SMTP_SERVER_SETTINGS,
        )

        val result = accountState.toOutgoingServerSettingsState()

        assertThat(result).isEqualTo(OUTGOING_STATE.copy(isLoading = false))
    }

    @Test
    fun `should use password from incomingServerSettings when outgoingServerSettings is missing a password`() {
        val accountState = AccountState(
            incomingServerSettings = IMAP_SERVER_SETTINGS,
            outgoingServerSettings = SMTP_SERVER_SETTINGS.copy(password = null),
        )

        val result = accountState.toOutgoingServerSettingsState()

        assertThat(result).isEqualTo(
            OUTGOING_STATE.copy(
                password = StringInputField(value = INCOMING_SERVER_PASSWORD),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `should use empty password if neither incomingServerSettings nor outgoingServerSettings contain passwords`() {
        val accountState = AccountState(
            outgoingServerSettings = SMTP_SERVER_SETTINGS.copy(password = null),
        )

        val result = accountState.toOutgoingServerSettingsState()

        assertThat(result).isEqualTo(
            OUTGOING_STATE.copy(
                password = StringInputField(value = ""),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `should map state to server settings and trim input`() {
        val outgoingState = OUTGOING_STATE.copy(
            server = StringInputField(value = " smtp.example.org "),
            username = StringInputField(value = " user "),
            password = StringInputField(value = " password "),
        )

        val result = outgoingState.toServerSettings()

        assertThat(result).isEqualTo(SMTP_SERVER_SETTINGS)
    }

    private companion object {
        private val OUTGOING_STATE = State(
            server = StringInputField(value = "smtp.example.org"),
            port = NumberInputField(value = 587),
            security = ConnectionSecurity.TLS,
            authenticationType = AuthenticationType.PasswordCleartext,
            username = StringInputField(value = "user"),
            password = StringInputField(value = "password"),
            clientCertificateAlias = null,
        )

        private val SMTP_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.org",
            port = 587,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        private const val INCOMING_SERVER_PASSWORD = "incoming-password"
        private val IMAP_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.domain.example",
            port = 993,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = INCOMING_SERVER_PASSWORD,
            clientCertificateAlias = null,
        )
    }
}
