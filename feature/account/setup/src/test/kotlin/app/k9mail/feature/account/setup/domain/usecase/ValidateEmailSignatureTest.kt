package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import org.junit.Test

class ValidateEmailSignatureTest {

    private val testSubject = ValidateEmailSignature()

    @Test
    fun `should succeed when email signature is set`() {
        val result = testSubject.execute("email signature")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should succeed when email signature is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when email signature is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateEmailSignatureError.BlankEmailSignature>()
    }
}
