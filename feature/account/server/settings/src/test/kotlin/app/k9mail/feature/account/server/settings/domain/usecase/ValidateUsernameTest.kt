package app.k9mail.feature.account.server.settings.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import org.junit.Test

class ValidateUsernameTest {

    private val testSubject = ValidateUsername()

    @Test
    fun `should succeed when username is set`() {
        val result = testSubject.execute("username")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when username is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateUsername.ValidateUsernameError.EmptyUsername>()
    }

    @Test
    fun `should fail when username is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateUsername.ValidateUsernameError.EmptyUsername>()
    }
}
