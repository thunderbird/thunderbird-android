package app.k9mail.feature.account.setup.ui.autodiscovery

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

class FakeAccountAutoDiscoveryValidator(
    private val emailAddressAnswer: ValidationOutcome = ValidationSuccess,
    private val passwordAnswer: ValidationOutcome = ValidationSuccess,
    private val proxyServerAnswer: ValidationOutcome = ValidationSuccess,
    private val proxyPortAnswer: ValidationOutcome = ValidationSuccess,
    private val configurationApprovalAnswer: ValidationOutcome = ValidationSuccess,
) : AccountAutoDiscoveryContract.Validator {
    override fun validateEmailAddress(emailAddress: String): ValidationOutcome = emailAddressAnswer
    override fun validatePassword(password: String): ValidationOutcome = passwordAnswer
    override fun validateProxyServer(server: String): ValidationOutcome = proxyServerAnswer
    override fun validateProxyPort(port: Long?): ValidationOutcome = proxyPortAnswer
    override fun validateConfigurationApproval(
        isApproved: Boolean?,
        isAutoDiscoveryTrusted: Boolean?,
    ): ValidationOutcome = configurationApprovalAnswer
}
