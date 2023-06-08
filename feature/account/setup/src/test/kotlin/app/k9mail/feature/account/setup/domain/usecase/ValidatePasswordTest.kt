package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidatePasswordTest {

    @Test
    fun `should succeed when password is set`() = runTest {
        val useCase = ValidatePassword()

        val result = useCase.execute("password")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when password is empty`() = runTest {
        val useCase = ValidatePassword()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePassword.ValidatePasswordError.EmptyPassword::class)
    }

    @Test
    fun `should fail when password is blank`() = runTest {
        val useCase = ValidatePassword()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidatePassword.ValidatePasswordError.EmptyPassword::class)
    }
}
