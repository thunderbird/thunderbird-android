package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidatePasswordTest {

    private val testSubject = ValidatePassword()

    @Test
    fun `should succeed when password is set`() {
        val result = testSubject.execute("password")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when password is blank`() {
        val testCases = listOf(
            "",
            "\n",
            " ",
        )

        testCases.forEach { testCase ->
            assertThat(testSubject.execute(testCase))
                .isInstanceOf<ValidationResult.Failure>()
                .prop(ValidationResult.Failure::error)
                .isInstanceOf<ValidatePassword.ValidatePasswordError.EmptyPassword>()
        }
    }

    @Test
    fun `should fail when password contains linebreak`() {
        val testCases = listOf(
            "password\n",
            "\npassword",
        )

        testCases.forEach { testCase ->
            assertThat(testSubject.execute(testCase))
                .isInstanceOf<ValidationResult.Failure>()
                .prop(ValidationResult.Failure::error)
                .isInstanceOf<ValidatePassword.ValidatePasswordError.LinebreakInPassword>()
        }
    }
}
