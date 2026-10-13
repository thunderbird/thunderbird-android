package app.k9mail.feature.account.oauth.ui

import android.content.res.Resources
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import net.openid.appauth.AuthorizationException

internal fun Error.toResourceString(resources: Resources): String {
    return when (this) {
        Error.BrowserNotAvailable -> resources.getString(R.string.account_oauth_error_browser_not_available)
        Error.Canceled -> resources.getString(R.string.account_oauth_error_canceled)
        Error.NotSupported -> resources.getString(R.string.account_oauth_error_not_supported)
        is Error.Unknown -> resources.getString(R.string.account_oauth_error_failed, error.toOAuthErrorMessage())
    }
}

internal fun Exception.toOAuthErrorMessage(): String = when (this) {
    is AuthorizationException -> errorDescription ?: error ?: "OAuth error (type $type, code $code)"

    else -> message?.takeIf(String::isNotBlank)
        ?: javaClass.simpleName.takeIf(String::isNotBlank)
        ?: "Unknown error"
}
