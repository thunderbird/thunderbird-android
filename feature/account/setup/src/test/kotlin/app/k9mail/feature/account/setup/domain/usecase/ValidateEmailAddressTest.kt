package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress.ValidateEmailAddressError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import org.junit.Before
import org.junit.Test

class ValidateEmailAddressTest {

    private val testSubject = ValidateEmailAddress()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `should succeed when email address is valid`() {
        val result = testSubject.execute("test@example.com")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when email address is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.EmptyEmailAddress>()
    }

    @Test
    fun `should fail when email address is using unnecessary quoting in local part`() {
        val result = testSubject.execute("\"local-part\"@domain.example")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.NotAllowed>()
    }

    @Test
    fun `should fail when email address requires quoted local part`() {
        val result = testSubject.execute("\"local part\"@domain.example")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.NotAllowed>()
    }

    @Test
    fun `should fail when local part is empty`() {
        val result = testSubject.execute("\"\"@domain.example")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.NotAllowed>()
    }

    @Test
    fun `should fail when domain part contains IPv4 literal`() {
        val result = testSubject.execute("user@[255.0.100.23]")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.NotAllowed>()
    }

    @Test
    fun `should fail when domain part contains IPv6 literal`() {
        val result = testSubject.execute("user@[IPv6:2001:0db8:0000:0000:0000:ff00:0042:8329]")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.NotAllowed>()
    }

    @Test
    fun `should fail when local part contains non-ASCII character`() {
        val result = testSubject.execute("töst@domain.example")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.InvalidOrNotSupported>()
    }

    @Test
    fun `should fail when domain contains non-ASCII character`() {
        val result = testSubject.execute("test@dömain.example")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.InvalidOrNotSupported>()
    }

    @Test
    fun `should fail when email address is invalid`() {
        val result = testSubject.execute("test")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateEmailAddressError.InvalidEmailAddress>()
    }
}
