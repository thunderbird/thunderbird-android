package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateImapPrefixTest {

    @Test
    fun `should success when imap prefix is set`() {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute("imap")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when imap prefix is empty`() {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when imap prefix is blank`() {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateImapPrefix.ValidateImapPrefixError.BlankImapPrefix::class)
    }
}
