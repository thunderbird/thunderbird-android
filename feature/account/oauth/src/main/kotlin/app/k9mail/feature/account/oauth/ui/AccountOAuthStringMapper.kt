package app.k9mail.feature.account.oauth.ui

import android.content.res.Resources
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        Error.BrowserNotAvailable -> resources.getString(R.string.account_oauth_error_browser_not_available)
        Error.Canceled -> resources.getString(R.string.account_oauth_error_canceled)
        Error.NotSupported -> resources.getString(R.string.account_oauth_error_not_supported)
        is Error.Unknown -> resources.getString(R.string.account_oauth_error_failed, error.message)
    }
}
