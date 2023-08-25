package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName.ValidateAccountNameError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateAccountNameTest {

    private val testSubject = ValidateAccountName()

    @Test
    fun `should succeed when account name is set`() {
        val result = testSubject.execute("account name")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should succeed when account name is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when account name is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateAccountNameError.BlankAccountName>()
    }
}
