package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AccountIncomingConfigStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                protocolType = IncomingProtocolType.IMAP,
                server = StringInputField(),
                security = ConnectionSecurity.DEFAULT,
                port = NumberInputField(value = ConnectionSecurity.DEFAULT.toImapDefaultPort()),
                username = StringInputField(),
                password = StringInputField(),
                clientCertificate = "",
                imapAutodetectNamespaceEnabled = true,
                imapUseCompression = true,
            ),
        )
    }
}
