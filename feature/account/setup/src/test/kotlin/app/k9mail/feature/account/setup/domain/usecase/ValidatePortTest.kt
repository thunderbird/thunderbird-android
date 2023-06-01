package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidatePort.ValidatePortError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidatePortTest {

    @Test
    fun `should succeed when port is set`() {
        val useCase = ValidatePort()

        val result = useCase.execute(123L)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when port is negative`() {
        val useCase = ValidatePort()

        val result = useCase.execute(-1L)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.InvalidPort::class)
    }

    @Test
    fun `should fail when port exceeds maximum`() {
        val useCase = ValidatePort()

        val result = useCase.execute(65536L)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.InvalidPort::class)
    }

    @Test
    fun `should fail when port is null`() {
        val useCase = ValidatePort()

        val result = useCase.execute(null)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePortError.EmptyPort::class)
    }
}
