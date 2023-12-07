package app.k9mail.feature.account.server.validation.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import app.k9mail.feature.account.server.validation.R
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        is Error.CertificateError -> error("Handle CertificateError using ServerCertificateErrorScreen")

        is Error.AuthenticationError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_authentication,
                detailsResId = R.string.account_server_validation_error_server_message,
                detailsMessage = serverMessage,
            )
        }

        is Error.NetworkError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_network,
                detailsResId = R.string.account_server_validation_error_details,
                detailsMessage = exception.message,
            )
        }

        is Error.ServerError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_server,
                detailsResId = R.string.account_server_validation_error_server_message,
                detailsMessage = serverMessage,
            )
        }

        is Error.UnknownError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_unknown,
                detailsResId = R.string.account_server_validation_error_details,
                detailsMessage = message,
            )
        }
    }
}

private fun Resources.buildErrorString(
    @StringRes titleResId: Int,
    @StringRes detailsResId: Int,
    detailsMessage: String?,
): String {
    val title = getString(titleResId)
    return if (detailsMessage != null) {
        val details = getString(detailsResId, detailsMessage)
        "$title\n\n$details"
    } else {
        title
    }
}
