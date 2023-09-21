package app.k9mail.core.common.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutcomeOnExtensionsKtTest {

    @Test
    fun `should invoke onSuccess action when success`() {
        val success = Success("success")
        var invoked = false

        success onSuccess {
            invoked = true
        }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `should not invoke onSuccess action when failure`() {
        val failure = Failure("failure")
        var invoked = false

        failure onSuccess {
            invoked = true
        }

        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `should invoke onFailure action when failure`() {
        val failure = Failure("failure")
        var invoked = false

        failure onFailure {
            invoked = true
        }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `should not invoke onFailure action when success`() {
        val success = Success("success")
        var invoked = false

        success onFailure {
            invoked = true
        }

        assertThat(invoked).isEqualTo(false)
    }
}
