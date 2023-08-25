package app.k9mail.feature.account.oauth.ui

import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.Test

class AccountOAuthStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).all {
            prop(State::hostname).isEqualTo("")
            prop(State::emailAddress).isEqualTo("")
            prop(State::wizardNavigationBarState).isEqualTo(
                WizardNavigationBarState(
                    isNextEnabled = false,
                ),
            )
            prop(State::isGoogleSignIn).isEqualTo(false)
            prop(State::error).isEqualTo(null)
            prop(State::isLoading).isEqualTo(false)
        }
    }
}
