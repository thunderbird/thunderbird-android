package app.k9mail.feature.account.setup.ui.autoconfig

import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.Test

class AccountAutoConfigStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::configStep).isEqualTo(ConfigStep.EMAIL_ADDRESS)
            prop(State::emailAddress).isEqualTo(StringInputField())
            prop(State::password).isEqualTo(StringInputField())
            prop(State::autoConfig).isEqualTo(null)
            prop(State::errorMessage).isEqualTo(null)
            prop(State::isLoading).isEqualTo(false)
        }
    }
}
