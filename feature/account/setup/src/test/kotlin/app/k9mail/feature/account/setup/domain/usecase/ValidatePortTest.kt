package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidatePort.ValidatePortError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidatePortTest {

    @Test
    fun `should succeed when port is set`() = runTest {
        val useCase = ValidatePort()

        val result = useCase.execute(123L)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when port is negative`() = runTest {
        val useCase = ValidatePort()

        val result = useCase.execute(-1L)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.InvalidPort::class)
    }

    @Test
    fun `should fail when port is zero`() = runTest {
        val useCase = ValidatePort()

        val result = useCase.execute(0)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.InvalidPort::class)
    }

    @Test
    fun `should fail when port exceeds maximum`() = runTest {
        val useCase = ValidatePort()

        val result = useCase.execute(65536L)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.InvalidPort::class)
    }

    @Test
    fun `should fail when port is null`() = runTest {
        val useCase = ValidatePort()

        val result = useCase.execute(null)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.EmptyPort::class)
    }
}
