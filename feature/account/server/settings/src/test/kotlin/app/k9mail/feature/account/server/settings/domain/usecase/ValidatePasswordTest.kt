package app.k9mail.feature.account.server.settings.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import org.junit.Test

class ValidatePasswordTest {

    private val testSubject = ValidatePassword()

    @Test
    fun `should succeed when password is set`() {
        val result = testSubject.execute("password")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when password is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePassword.ValidatePasswordError.EmptyPassword>()
    }

    @Test
    fun `should fail when password is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidatePassword.ValidatePasswordError.EmptyPassword>()
    }
}
