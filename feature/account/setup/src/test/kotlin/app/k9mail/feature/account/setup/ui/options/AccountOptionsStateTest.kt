package app.k9mail.feature.account.setup.ui.options

import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.Test

class AccountOptionsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::accountName).isEqualTo(StringInputField())
            prop(State::displayName).isEqualTo(StringInputField())
            prop(State::emailSignature).isEqualTo(StringInputField())
            prop(State::checkFrequency).isEqualTo(EmailCheckFrequency.DEFAULT)
            prop(State::messageDisplayCount).isEqualTo(EmailDisplayCount.DEFAULT)
            prop(State::showNotification).isEqualTo(false)
        }
    }
}
