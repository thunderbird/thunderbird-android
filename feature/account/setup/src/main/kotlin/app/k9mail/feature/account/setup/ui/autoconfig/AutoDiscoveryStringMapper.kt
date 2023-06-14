package app.k9mail.feature.account.setup.ui.autoconfig

import android.content.res.Resources
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoveryConnectionSecurity

internal fun AutoDiscoveryConnectionSecurity.toResourceString(resources: Resources): String {
    return when (this) {
        AutoDiscoveryConnectionSecurity.StartTLS -> resources.getString(
            R.string.account_setup_connection_security_start_tls,
        )
        AutoDiscoveryConnectionSecurity.TLS -> resources.getString(
            R.string.account_setup_connection_security_ssl,
        )
    }
}

internal fun AccountAutoConfigContract.Error.toResourceString(resources: Resources): String {
    return when (this) {
        AccountAutoConfigContract.Error.NetworkError -> resources.getString(R.string.account_setup_error_network)
        AccountAutoConfigContract.Error.UnknownError -> resources.getString(R.string.account_setup_error_unknown)
    }
}
