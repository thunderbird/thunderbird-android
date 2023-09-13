package app.k9mail.feature.account.server.settings.ui.common.mapper

import android.content.res.Resources
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.server.settings.R

internal fun ConnectionSecurity.toResourceString(resources: Resources): String {
    return when (this) {
        ConnectionSecurity.None -> resources.getString(R.string.account_server_settings_connection_security_none)
        ConnectionSecurity.StartTLS -> resources.getString(
            R.string.account_server_settings_connection_security_start_tls,
        )
        ConnectionSecurity.TLS -> resources.getString(R.string.account_server_settings_connection_security_ssl)
    }
}
