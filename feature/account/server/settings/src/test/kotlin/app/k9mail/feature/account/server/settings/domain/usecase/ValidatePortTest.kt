package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePort.ValidatePortError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidatePortTest {

    private val testSubject = ValidatePort()

    @Test
    fun `should succeed when port is set`() {
        val result = testSubject.execute(123L)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when port is negative`() {
        val result = testSubject.execute(-1L)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePortError.InvalidPort>()
    }

    @Test
    fun `should fail when port is zero`() {
        val result = testSubject.execute(0)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePortError.InvalidPort>()
    }

    @Test
    fun `should fail when port exceeds maximum`() {
        val result = testSubject.execute(65536L)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePortError.InvalidPort>()
    }

    @Test
    fun `should fail when port is null`() {
        val result = testSubject.execute(null)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePortError.EmptyPort>()
    }
}
