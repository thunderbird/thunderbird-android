package app.k9mail.feature.account.setup.ui.autoconfig

import android.content.res.Resources
import app.k9mail.feature.account.setup.R

internal fun AccountAutoConfigContract.Error.toResourceString(resources: Resources): String {
    return when (this) {
        AccountAutoConfigContract.Error.NetworkError -> resources.getString(R.string.account_setup_error_network)
        AccountAutoConfigContract.Error.UnknownError -> resources.getString(R.string.account_setup_error_unknown)
    }
}
