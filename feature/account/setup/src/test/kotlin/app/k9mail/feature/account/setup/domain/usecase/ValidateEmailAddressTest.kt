package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress.ValidateEmailAddressError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateEmailAddressTest {

    @Test
    fun `should succeed when email address is valid`() {
        val useCase = ValidateEmailAddress()

        val result = useCase.execute("test@example.com")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when email address is blank`() {
        val useCase = ValidateEmailAddress()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateEmailAddressError.EmptyEmailAddress::class)
    }

    @Test
    fun `should fail when email address is invalid`() {
        val useCase = ValidateEmailAddress()

        val result = useCase.execute("test")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateEmailAddressError.InvalidEmailAddress::class)
    }
}
