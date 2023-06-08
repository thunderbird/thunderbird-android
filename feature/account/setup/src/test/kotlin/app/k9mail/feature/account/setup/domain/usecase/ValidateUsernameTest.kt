package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateUsernameTest {

    @Test
    fun `should succeed when username is set`() = runTest {
        val useCase = ValidateUsername()

        val result = useCase.execute("username")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when username is empty`() = runTest {
        val useCase = ValidateUsername()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateUsername.ValidateUsernameError.EmptyUsername::class)
    }

    @Test
    fun `should fail when username is blank`() = runTest {
        val useCase = ValidateUsername()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateUsername.ValidateUsernameError.EmptyUsername::class)
    }
}
