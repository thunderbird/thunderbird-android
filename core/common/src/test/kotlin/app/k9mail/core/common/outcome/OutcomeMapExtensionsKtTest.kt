package app.k9mail.core.common.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutcomeMapExtensionsKtTest {

    @Test
    fun `should map success and failure outcome to new outcome`() {
        val success = Success("success")
        val failure = Failure("failure")

        val transformSuccess: (String) -> Int = { 1 }
        val transformFailure: (String) -> Exception = { Exception("failure exception") }

        val mappedSuccess = success.map(transformSuccess, transformFailure)
        val mappedFailure = failure.map(transformSuccess, transformFailure)

        assertThat(mappedSuccess).isEqualTo(Success(1))
        assertThat(mappedFailure.unwrapError().message).isEqualTo("failure exception")
    }

    @Test
    fun `mapValue should map value of success outcome to new outcome`() {
        val success = Success("success")

        val mappedSuccess = success mapValue { 1 }

        assertThat(mappedSuccess).isEqualTo(Success(1))
    }

    @Test
    fun `mapValue should keep failure`() {
        val failure = Failure("failure")

        val mappedFailure = failure mapValue { 1 }

        assertThat(mappedFailure).isEqualTo(failure)
    }

    @Test
    fun `mapError should map error of failure outcome to new outcome`() {
        val failure = Failure("failure")

        val mappedFailure = failure mapError { Exception("failure exception") }

        assertThat(mappedFailure.unwrapError().message).isEqualTo("failure exception")
    }

    @Test
    fun `mapError should keep success`() {
        val success = Success("success")

        val mappedSuccess = success mapError { Exception("failure exception") }

        assertThat(mappedSuccess).isEqualTo(success)
    }
}
