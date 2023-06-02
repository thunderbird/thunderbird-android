package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.junit.Test

class AccountOutgoingConfigStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::server).isEqualTo(StringInputField())
            prop(State::security).isEqualTo(ConnectionSecurity.DEFAULT)
            prop(State::port).isEqualTo(NumberInputField(value = ConnectionSecurity.DEFAULT.toSmtpDefaultPort()))
            prop(State::username).isEqualTo(StringInputField())
            prop(State::password).isEqualTo(StringInputField())
            prop(State::clientCertificate).isEqualTo("")
            prop(State::imapAutodetectNamespaceEnabled).isTrue()
            prop(State::useCompression).isTrue()
        }
    }
}
