package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.junit.Test

class AccountIncomingConfigStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::protocolType).isEqualTo(IncomingProtocolType.IMAP)
            prop(State::server).isEqualTo(StringInputField())
            prop(State::security).isEqualTo(ConnectionSecurity.DEFAULT)
            prop(State::port).isEqualTo(NumberInputField(value = ConnectionSecurity.DEFAULT.toImapDefaultPort()))
            prop(State::username).isEqualTo(StringInputField())
            prop(State::password).isEqualTo(StringInputField())
            prop(State::clientCertificate).isEqualTo("")
            prop(State::imapAutodetectNamespaceEnabled).isTrue()
            prop(State::useCompression).isTrue()
        }
    }
}
