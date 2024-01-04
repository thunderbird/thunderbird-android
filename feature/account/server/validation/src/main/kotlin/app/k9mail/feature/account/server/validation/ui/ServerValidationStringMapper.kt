package app.k9mail.feature.account.server.validation.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import app.k9mail.feature.account.server.validation.R
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error
import app.k9mail.feature.account.common.R as CommonR

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        is Error.CertificateError -> error("Handle CertificateError using ServerCertificateErrorScreen")

        is Error.AuthenticationError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_authentication,
                detailsResId = CommonR.string.account_common_error_server_message,
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
                detailsResId = CommonR.string.account_common_error_server_message,
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

        Error.ClientCertificateExpired -> {
            resources.getString(R.string.account_server_validation_error_client_certificate_expired)
        }

        Error.ClientCertificateRetrievalFailure -> {
            resources.getString(R.string.account_server_validation_error_client_certificate_retrieval_failure)
        }

        is Error.MissingServerCapabilityError -> {
            resources.buildErrorString(
                titleResId = R.string.account_server_validation_error_missing_server_capability,
                detailsResId = R.string.account_server_validation_error_missing_server_capability_details,
                detailsMessage = capabilityName,
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
