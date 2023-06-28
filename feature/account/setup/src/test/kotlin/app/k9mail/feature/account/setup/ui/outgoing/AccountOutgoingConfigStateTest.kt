package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AccountOutgoingConfigStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                server = StringInputField(),
                security = ConnectionSecurity.DEFAULT,
                port = NumberInputField(value = ConnectionSecurity.DEFAULT.toSmtpDefaultPort()),
                username = StringInputField(),
                password = StringInputField(),
                clientCertificate = "",
            ),
        )
    }
}
