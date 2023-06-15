package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateImapPrefixTest {

    private val testSubject = ValidateImapPrefix()

    @Test
    fun `should success when imap prefix is set`() {
        val result = testSubject.execute("imap")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when imap prefix is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when imap prefix is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateImapPrefix.ValidateImapPrefixError.BlankImapPrefix::class)
    }
}
