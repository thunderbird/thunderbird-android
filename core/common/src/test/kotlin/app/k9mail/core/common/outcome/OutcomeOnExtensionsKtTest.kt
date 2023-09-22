package app.k9mail.core.common.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutcomeOnExtensionsKtTest {

    @Test
    fun `should invoke onSuccess action when success`() {
        val success = Success("success")
        var invoked = false
        var value: String? = null

        success onSuccess {
            invoked = true
            value = it
        }

        assertThat(invoked).isEqualTo(true)
        assertThat(value).isEqualTo("success")
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
        val failure = Failure("failure", "cause")
        var invoked = false
        var failureError: String? = null
        var failureCause: Any? = null

        failure onFailure { error, cause ->
            invoked = true
            failureError = error
            failureCause = cause
        }

        assertThat(invoked).isEqualTo(true)
        assertThat(failureError).isEqualTo("failure")
        assertThat(failureCause).isEqualTo("cause")
    }

    @Test
    fun `should not invoke onFailure action when success`() {
        val success = Success("success")
        var invoked = false

        success onFailure { _, _ ->
            invoked = true
        }

        assertThat(invoked).isEqualTo(false)
    }
}
