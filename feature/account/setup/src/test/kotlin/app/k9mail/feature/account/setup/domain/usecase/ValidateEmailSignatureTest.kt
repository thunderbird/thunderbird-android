package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import org.junit.Test

class ValidateEmailSignatureTest {

    private val testSubject = ValidateEmailSignature()

    @Test
    fun `should succeed when email signature is set`() {
        val result = testSubject.execute("email signature")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should succeed when email signature is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when email signature is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailSignatureError.BlankEmailSignature>()
    }
}
