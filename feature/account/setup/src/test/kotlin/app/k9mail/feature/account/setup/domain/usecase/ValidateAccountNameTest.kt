package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName.ValidateAccountNameError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import org.junit.Test

class ValidateAccountNameTest {

    private val testSubject = ValidateAccountName()

    @Test
    fun `should succeed when account name is set`() {
        val result = testSubject.execute("account name")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should succeed when account name is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when account name is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAccountNameError.BlankAccountName>()
    }
}
