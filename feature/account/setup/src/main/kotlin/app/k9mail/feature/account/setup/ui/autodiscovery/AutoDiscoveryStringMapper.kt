package app.k9mail.feature.account.setup.ui.autodiscovery

import android.content.res.Resources
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoveryConnectionSecurity
import app.k9mail.feature.account.setup.domain.usecase.ValidateConfigurationApproval
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress

internal fun AutoDiscoveryConnectionSecurity.toResourceString(resources: Resources): String {
    return when (this) {
        AutoDiscoveryConnectionSecurity.StartTLS -> resources.getString(
            R.string.account_setup_auto_discovery_connection_security_start_tls,
        )

        AutoDiscoveryConnectionSecurity.TLS -> resources.getString(
            R.string.account_setup_auto_discovery_connection_security_ssl,
        )
    }
}

internal fun AccountAutoDiscoveryContract.Error.toResourceString(resources: Resources): String {
    return when (this) {
        AccountAutoDiscoveryContract.Error.NetworkError -> resources.getString(R.string.account_setup_error_network)
        AccountAutoDiscoveryContract.Error.UnknownError -> resources.getString(R.string.account_setup_error_unknown)
    }
}

internal fun ValidationError.toResourceString(resources: Resources): String {
    return when (this) {
        is ValidateEmailAddress.ValidateEmailAddressError -> toEmailAddressErrorString(resources)
        is ValidateConfigurationApproval.ValidateConfigurationApprovalError -> toConfigurationApprovalErrorString(
            resources,
        )

        else -> throw IllegalArgumentException("Unknown error: $this")
    }
}

private fun ValidateEmailAddress.ValidateEmailAddressError.toEmailAddressErrorString(resources: Resources): String {
    return when (this) {
        is ValidateEmailAddress.ValidateEmailAddressError.EmptyEmailAddress -> resources.getString(
            R.string.account_setup_auto_discovery_validation_error_email_address_required,
        )

        is ValidateEmailAddress.ValidateEmailAddressError.InvalidEmailAddress -> resources.getString(
            R.string.account_setup_auto_discovery_validation_error_email_address_invalid,
        )
    }
}

private fun ValidateConfigurationApproval.ValidateConfigurationApprovalError.toConfigurationApprovalErrorString(
    resources: Resources,
): String {
    return when (this) {
        ValidateConfigurationApproval.ValidateConfigurationApprovalError.ApprovalRequired -> resources.getString(
            R.string.account_setup_auto_discovery_result_approval_error_approval_required,
        )
    }
}
