package app.k9mail.core.common.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutcomeMapExtensionsKtTest {

    @Test
    fun `should map success and failure outcome to new outcome`() {
        val success = Success("success")
        val failure = Failure("failure", "cause")

        val transformSuccess: (String) -> Int = { 1 }
        val transformFailure: (String, Any?) -> Exception = { error, cause ->
            Exception("error: $error, cause: $cause")
        }

        val mappedSuccess = success.map(transformSuccess, transformFailure)
        val mappedFailure = failure.map(transformSuccess, transformFailure)

        assertThat(mappedSuccess).isEqualTo(Success(1))
        assertThat(mappedFailure.unwrapError().message).isEqualTo("error: failure, cause: cause")
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

        val mappedFailure = failure mapError { _, _ -> Exception("failure exception") }

        assertThat(mappedFailure.unwrapError().message).isEqualTo("failure exception")
    }

    @Test
    fun `mapError should keep success`() {
        val success = Success("success")

        val mappedSuccess = success mapError { _, _ -> Exception("failure exception") }

        assertThat(mappedSuccess).isEqualTo(success)
    }
}
