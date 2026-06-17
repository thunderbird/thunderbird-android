package app.k9mail.feature.account.server.settings.ui.common.mapper

import android.content.res.Resources
import app.k9mail.feature.account.server.settings.R
import com.fsck.k9.mail.MailProxyType

internal fun MailProxyType.toResourceString(resources: Resources): String {
    return when (this) {
        MailProxyType.NONE -> resources.getString(R.string.account_server_settings_proxy_type_none)
        MailProxyType.HTTP -> resources.getString(R.string.account_server_settings_proxy_type_http)
        MailProxyType.SOCKS4 -> resources.getString(R.string.account_server_settings_proxy_type_socks4)
        MailProxyType.SOCKS5 -> resources.getString(R.string.account_server_settings_proxy_type_socks5)
    }
}
