package net.thunderbird.core.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class OutcomeTest {

    @Test
    fun `given Success when checking then has correct flags and data`() {
        // Arrange
        val outcome: Outcome<Int, String> = Outcome.success(42)

        // Act + Assert
        assertThat(outcome.isSuccess).isTrue()
        assertThat(outcome.isFailure).isFalse()
        val data = (outcome as Outcome.Success).data
        assertThat(data).isEqualTo(42)
    }

    @Test
    fun `given Failure when checking then has correct flags and error`() {
        // Arrange
        val outcome: Outcome<Int, String> = Outcome.failure("error")

        // Act + Assert
        assertThat(outcome.isFailure).isTrue()
        assertThat(outcome.isSuccess).isFalse()
        val error = (outcome as Outcome.Failure).error
        assertThat(error).isEqualTo("error")
    }

    @Test
    fun `given Success when map is called then transforms success value`() {
        // Arrange
        val outcome: Outcome<Int, String> = Outcome.Success(7)

        // Act
        val mapped = outcome.map(
            transformSuccess = { it * 2 },
            transformFailure = { err, _ -> "$err!" },
        )

        // Assert
        val data = (mapped as Outcome.Success).data
        assertThat(data).isEqualTo(14)
    }

    @Test
    fun `given Failure with cause when map is called then transforms failure value and provides cause`() {
        // Arrange
        val cause = IllegalStateException("cause")
        val outcome: Outcome<Int, String> = Outcome.Failure("error", cause)

        // Act
        val mapped = outcome.map(
            transformSuccess = { it * 2 },
            transformFailure = { err, receivedCause ->
                assertThat(receivedCause).isEqualTo(cause)
                "$err-transformed"
            },
        )

        // Assert
        val failure = (mapped as Outcome.Failure)
        assertThat(failure.error).isEqualTo("error-transformed")
    }

    @Test
    fun `given Success and Failure when mapSuccess is called then only success is transformed`() {
        // Arrange & Act
        val success = Outcome.Success(3).mapSuccess { it + 1 }
        val failure = Outcome.Failure("failure").mapSuccess { 999 }

        // Assert
        assertThat((success as Outcome.Success).data).isEqualTo(4)
        // Failure must be unchanged
        assertThat((failure as Outcome.Failure).error).isEqualTo("failure")
    }

    @Test
    fun `given Success and Failure when flatMapSuccess is called then success is flat-mapped and failure passes through`() {
        // Arrange & Act
        val onSuccess = Outcome.Success(10).flatMapSuccess { value ->
            if (value > 5) Outcome.Success("success") else Outcome.Failure("failure")
        }
        val onFailure: Outcome<String, String> =
            Outcome.Failure("failure").flatMapSuccess { Outcome.Success("won't happen") }

        // Assert
        assertThat((onSuccess as Outcome.Success).data).isEqualTo("success")
        assertThat((onFailure as Outcome.Failure).error).isEqualTo("failure")
    }

    @Test
    fun `given Success and Failure when mapFailure is called then only failure is transformed and cause provided`() {
        // Arrange & Act
        val cause = RuntimeException("cause")
        val success = Outcome.Success("success").mapFailure { e: String, _ -> "$e?" }
        val failure = Outcome.Failure("fail", cause).mapFailure { e, c ->
            assertThat(c).isEqualTo(cause)
            999
        }

        // Assert
        assertThat((success as Outcome.Success).data).isEqualTo("success")
        assertThat((failure as Outcome.Failure).error).isEqualTo(999)
    }

    @Test
    fun `given Outcome when handle is invoked then calls only the matching callback`() {
        // Arrange
        var successCalledWith: Int? = null
        var failureCalledWith: String? = null

        // Act
        Outcome.Success(5).handle(
            onSuccess = { successCalledWith = it },
            onFailure = { failureCalledWith = it },
        )
        // Assert
        assertThat(successCalledWith).isEqualTo(5)
        assertThat(failureCalledWith).isNull()

        // Arrange
        successCalledWith = null
        val failureOutcome: Outcome<Int, String> = Outcome.Failure("failure")
        // Act
        failureOutcome.handle(
            onSuccess = { successCalledWith = it },
            onFailure = { failureCalledWith = it },
        )
        // Assert
        assertThat(successCalledWith).isNull()
        assertThat(failureCalledWith).isEqualTo("failure")
    }

    @Test
    fun `given Outcome when handleAsync is invoked then calls only the matching suspending callback`() = runTest {
        // Arrange
        var successCalledWith: Int? = null
        var failureCalledWith: String? = null

        // Act
        Outcome.Success(1).handleAsync(
            onSuccess = { successCalledWith = it },
            onFailure = { failureCalledWith = it },
        )
        // Assert
        assertThat(successCalledWith).isEqualTo(1)
        assertThat(failureCalledWith).isNull()

        // Arrange
        successCalledWith = null
        val failureOutcome: Outcome<Int, String> = Outcome.Failure("failure")
        // Act
        failureOutcome.handleAsync(
            onSuccess = { successCalledWith = it },
            onFailure = { failureCalledWith = it },
        )
        // Assert
        assertThat(successCalledWith).isNull()
        assertThat(failureCalledWith).isEqualTo("failure")
    }
}
