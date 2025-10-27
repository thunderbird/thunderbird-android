package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class IncomingServerSettingsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                protocolType = IncomingProtocolType.IMAP,
                server = StringInputField(),
                security = ConnectionSecurity.DEFAULT,
                port = NumberInputField(value = ConnectionSecurity.DEFAULT.toImapDefaultPort()),
                authenticationType = AuthenticationType.PasswordCleartext,
                username = StringInputField(),
                password = StringInputField(),
                clientCertificateAlias = null,
                imapAutodetectNamespaceEnabled = true,
                imapUseCompression = true,
                imapSendClientInfo = true,
            ),
        )
    }
}
