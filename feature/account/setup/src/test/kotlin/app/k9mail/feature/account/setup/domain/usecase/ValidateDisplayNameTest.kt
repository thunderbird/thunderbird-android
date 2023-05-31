package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateDisplayName.ValidateDisplayNameError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateDisplayNameTest {

    @Test
    fun `should succeed when display name is set`() {
        val useCase = ValidateDisplayName()

        val result = useCase.execute("display name")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when display name is empty`() {
        val useCase = ValidateDisplayName()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateDisplayNameError.EmptyDisplayName::class)
    }

    @Test
    fun `should fail when display name is blank`() {
        val useCase = ValidateDisplayName()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateDisplayNameError.EmptyDisplayName::class)
    }
}
