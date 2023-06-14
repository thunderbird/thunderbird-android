package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateConfigurationApprovalTest {

    @Test
    fun `should succeed when auto discovery is approved and trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = true, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when auto discovery not approved but is trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = false, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should succeed when auto discovery is approved but not trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = true, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when auto discovery is not approved and not trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = false, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired::class)
    }

    @Test
    fun `should succeed when auto discovery isApproved null and is trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = null, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when auto discovery is isApproved null and is not trusted`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = null, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired::class)
    }

    @Test
    fun `should fail when auto discovery is approved and trusted is null`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = false, isAutoDiscoveryTrusted = null)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired::class)
    }

    @Test
    fun `should fail when auto discovery is not approved and trusted is null`() {
        val useCase = ValidateConfigurationApproval()

        val result = useCase.execute(isApproved = false, isAutoDiscoveryTrusted = null)

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired::class)
    }
}
