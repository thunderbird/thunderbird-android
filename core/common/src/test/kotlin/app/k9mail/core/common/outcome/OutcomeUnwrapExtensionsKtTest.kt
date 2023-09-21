package app.k9mail.core.common.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutcomeUnwrapExtensionsKtTest {

    @Test
    fun `should unwrap value of success outcome`() {
        val outcome = Success("success") as Outcome<String, String>

        val unwrappedValue = outcome.unwrapValue()

        assertThat(unwrappedValue).isEqualTo("success")
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error when unwrapping value of failure outcome`() {
        val outcome = Failure("failure") as Outcome<String, String>

        outcome.unwrapValue()
    }

    @Test
    fun `should unwrap error of failure outcome`() {
        val failure = Failure("failure") as Outcome<String, String>

        val unwrappedError = failure.unwrapError()

        assertThat(unwrappedError).isEqualTo("failure")
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error when unwrapping error of success outcome`() {
        val outcome = Success("success") as Outcome<String, String>

        outcome.unwrapError()
    }
}
