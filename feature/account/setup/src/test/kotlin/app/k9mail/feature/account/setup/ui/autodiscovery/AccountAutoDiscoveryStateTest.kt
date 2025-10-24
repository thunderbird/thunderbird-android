package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.validation.input.BooleanInputField
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class AccountAutoDiscoveryStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(),
                password = StringInputField(),
                autoDiscoverySettings = null,
                configurationApproved = BooleanInputField(),
                error = null,
                isLoading = false,
            ),
        )
    }
}
