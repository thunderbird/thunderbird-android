package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AccountValidationStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                serverSettings = null,
                isSuccess = false,
                error = null,
                isLoading = false,
            ),
        )
    }
}
