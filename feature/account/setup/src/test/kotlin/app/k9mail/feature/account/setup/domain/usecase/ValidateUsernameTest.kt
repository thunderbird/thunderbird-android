package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateUsernameTest {

    private val testSubject = ValidateUsername()

    @Test
    fun `should succeed when username is set`() {
        val result = testSubject.execute("username")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when username is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateUsername.ValidateUsernameError.EmptyUsername::class)
    }

    @Test
    fun `should fail when username is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateUsername.ValidateUsernameError.EmptyUsername::class)
    }
}
