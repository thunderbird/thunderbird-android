package app.k9mail.core.common.outcome

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OutcomeTest {

    @Test
    fun `should check for success`() {
        val outcome = Success("success")

        assertThat(outcome.isSuccess).isTrue()
        assertThat(outcome.isFailure).isFalse()
        assertThat(outcome.value).isEqualTo("success")
    }

    @Test
    fun `should check for failure`() {
        val outcome = Failure("failure")

        assertThat(outcome.isSuccess).isFalse()
        assertThat(outcome.isFailure).isTrue()
        assertThat(outcome.error).isEqualTo("failure")
    }

    @Test
    fun `should convert result to outcome`() {
        val result = Result.success("test")

        val outcome = result.asOutcome()

        assertThat(outcome)
            .isInstanceOf(Success::class)
            .prop("value") { Success<String>::value.call(it) }
            .isEqualTo("test")
    }

    @Test
    fun `should convert flow to outcome`() = runTest {
        flow {
            emit("test")
            throw IllegalArgumentException("some error")
        }.asOutcome()
            .test {
                assertThat(awaitItem()).isEqualTo(Success("test"))

                assertThat(awaitItem())
                    .isInstanceOf(Failure::class)
                    .prop("NAME") { Failure<Exception>::error.call(it) }
                    .isInstanceOf(IllegalArgumentException::class)
                    .prop(IllegalArgumentException::message)
                    .isEqualTo("some error")

                awaitComplete()
            }
    }
}
