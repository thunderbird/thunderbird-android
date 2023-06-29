package app.k9mail.feature.account.setup.ui.common.mapper

import android.content.res.Resources
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType

internal fun AuthenticationType.toResourceString(resources: Resources): String {
    return when (this) {
        AuthenticationType.None -> {
            resources.getString(R.string.account_setup_authentication_none)
        }
        AuthenticationType.PasswordCleartext -> {
            resources.getString(R.string.account_setup_authentication_password_cleartext)
        }
        AuthenticationType.PasswordEncrypted -> {
            resources.getString(R.string.account_setup_authentication_password_encrypted)
        }
        AuthenticationType.ClientCertificate -> {
            resources.getString(R.string.account_setup_authentication_client_certificate)
        }
        AuthenticationType.OAuth2 -> {
            resources.getString(R.string.account_setup_authentication_client_oauth)
        }
    }
}
