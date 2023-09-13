package app.k9mail.feature.account.server.settings.ui.common.mapper

import android.content.res.Resources
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.server.settings.R

internal fun AuthenticationType.toResourceString(resources: Resources): String {
    return when (this) {
        AuthenticationType.None -> {
            resources.getString(R.string.account_server_settings_authentication_none)
        }

        AuthenticationType.PasswordCleartext -> {
            resources.getString(R.string.account_server_settings_authentication_password_cleartext)
        }

        AuthenticationType.PasswordEncrypted -> {
            resources.getString(R.string.account_server_settings_authentication_password_encrypted)
        }

        AuthenticationType.ClientCertificate -> {
            resources.getString(R.string.account_server_settings_authentication_client_certificate)
        }

        AuthenticationType.OAuth2 -> {
            resources.getString(R.string.account_server_settings_authentication_client_oauth)
        }
    }
}
