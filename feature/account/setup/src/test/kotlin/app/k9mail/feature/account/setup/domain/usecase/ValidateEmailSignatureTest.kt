package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateEmailSignatureTest {

    @Test
    fun `should succeed when email signature is set`() = runTest {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute("email signature")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when email signature is empty`() = runTest {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when email signature is blank`() = runTest {
        val useCase = ValidateEmailSignature()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateEmailSignatureError.BlankEmailSignature::class)
    }
}
