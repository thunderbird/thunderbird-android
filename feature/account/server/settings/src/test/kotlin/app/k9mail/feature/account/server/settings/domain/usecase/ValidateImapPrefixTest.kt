package app.k9mail.feature.account.server.settings.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import org.junit.Test

class ValidateImapPrefixTest {

    private val testSubject = ValidateImapPrefix()

    @Test
    fun `should success when imap prefix is set`() {
        val result = testSubject.execute("imap")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should succeed when imap prefix is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when imap prefix is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateImapPrefix.ValidateImapPrefixError.BlankImapPrefix>()
    }
}
