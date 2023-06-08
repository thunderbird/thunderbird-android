package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateImapPrefixTest {

    @Test
    fun `should success when imap prefix is set`() = runTest {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute("imap")

        assertThat(result).isInstanceOf(
            ValidationResult.Success::class,
        )
    }

    @Test
    fun `should succeed when imap prefix is empty`() = runTest {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when imap prefix is blank`() = runTest {
        val useCase = ValidateImapPrefix()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateImapPrefix.ValidateImapPrefixError.BlankImapPrefix::class)
    }
}
