package app.k9mail.feature.account.server.validation.ui

import android.content.res.Resources
import app.k9mail.feature.account.server.validation.R
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        is Error.AuthenticationError -> resources.getString(
            R.string.account_server_validation_error_authentication,
        )

        is Error.CertificateError -> resources.getString(
            R.string.account_server_validation_error_certificate,
        )

        is Error.NetworkError -> resources.getString(
            R.string.account_server_validation_error_network,
        )

        is Error.ServerError -> resources.getString(
            R.string.account_server_validation_error_server,
        )

        is Error.UnknownError -> resources.getString(
            R.string.account_server_validation_error_unknown,
        )
    }
}
