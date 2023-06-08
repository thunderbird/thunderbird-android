package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName.ValidateAccountNameError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateAccountNameTest {

    @Test
    fun `should succeed when account name is set`() = runTest {
        val useCase = ValidateAccountName()

        val result = useCase.execute("account name")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when account name is empty`() = runTest {
        val useCase = ValidateAccountName()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when account name is blank`() = runTest {
        val useCase = ValidateAccountName()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateAccountNameError.BlankAccountName::class)
    }
}
