package net.thunderbird.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toSmtpDefaultPort
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import org.junit.Test

class OutgoingServerSettingsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                server = StringInputField(),
                security = ConnectionSecurity.DEFAULT,
                port = NumberInputField(value = ConnectionSecurity.DEFAULT.toSmtpDefaultPort()),
                authenticationType = AuthenticationType.PasswordCleartext,
                username = StringInputField(),
                password = StringInputField(),
                clientCertificateAlias = null,
            ),
        )
    }
}
