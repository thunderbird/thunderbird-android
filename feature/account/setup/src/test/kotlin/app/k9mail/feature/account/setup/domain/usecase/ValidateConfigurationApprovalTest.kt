package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateConfigurationApprovalTest {

    private val testSubject = ValidateConfigurationApproval()

    @Test
    fun `should succeed when auto discovery is approved and trusted`() {
        val result = testSubject.execute(isApproved = true, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should succeed when auto discovery not approved but is trusted`() {
        val result = testSubject.execute(isApproved = false, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should succeed when auto discovery is approved but not trusted`() {
        val result = testSubject.execute(isApproved = true, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when auto discovery is not approved and not trusted`() {
        val result = testSubject.execute(isApproved = false, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired>()
    }

    @Test
    fun `should succeed when auto discovery isApproved null and is trusted`() {
        val result = testSubject.execute(isApproved = null, isAutoDiscoveryTrusted = true)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when auto discovery is isApproved null and is not trusted`() {
        val result = testSubject.execute(isApproved = null, isAutoDiscoveryTrusted = false)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired>()
    }

    @Test
    fun `should fail when auto discovery is approved and trusted is null`() {
        val result = testSubject.execute(isApproved = false, isAutoDiscoveryTrusted = null)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired>()
    }

    @Test
    fun `should fail when auto discovery is not approved and trusted is null`() {
        val result = testSubject.execute(isApproved = false, isAutoDiscoveryTrusted = null)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired>()
    }
}
