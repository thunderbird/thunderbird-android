package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateEmailSignatureTest {

    @Test
    fun `should succeed when email signature is set`() {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute("email signature")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when email signature is empty`() {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateEmailSignature.EmptyEmailSignature::class)
    }

    @Test
    fun `should fail when email signature is blank`() {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateEmailSignature.EmptyEmailSignature::class)
    }
}
