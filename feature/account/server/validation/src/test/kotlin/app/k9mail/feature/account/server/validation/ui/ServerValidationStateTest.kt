package app.k9mail.feature.account.server.validation.ui

import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class ServerValidationStateTest {

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
