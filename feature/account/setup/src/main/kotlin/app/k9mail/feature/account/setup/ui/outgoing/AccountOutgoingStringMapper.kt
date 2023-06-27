package app.k9mail.feature.account.setup.ui.outgoing

import android.content.res.Resources
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Error

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        is Error.AuthenticationError -> resources.getString(
            R.string.account_setup_check_config_error_authentication,
        )

        is Error.CertificateError -> resources.getString(
            R.string.account_setup_check_config_error_certificate,
        )

        is Error.NetworkError -> resources.getString(
            R.string.account_setup_check_config_error_network,
        )

        is Error.ServerError -> resources.getString(
            R.string.account_setup_check_config_error_server,
        )

        is Error.UnknownError -> resources.getString(
            R.string.account_setup_check_config_error_unknown,
        )
    }
}
