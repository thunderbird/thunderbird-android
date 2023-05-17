package app.k9mail.feature.account.setup.ui

import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.Test

class AccountSetupStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::setupStep).isEqualTo(AccountSetupContract.SetupStep.AUTO_CONFIG)
        }
    }
}
