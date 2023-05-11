package app.k9mail.feature.account.setup.ui

import android.content.res.Resources
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity

internal fun ConnectionSecurity.toResourceString(resources: Resources): String {
    return when (this) {
        ConnectionSecurity.None -> resources.getString(R.string.account_setup_connection_security_none)
        ConnectionSecurity.StartTLS -> resources.getString(R.string.account_setup_connection_security_start_tls)
        ConnectionSecurity.TLS -> resources.getString(R.string.account_setup_connection_security_ssl)
    }
}
