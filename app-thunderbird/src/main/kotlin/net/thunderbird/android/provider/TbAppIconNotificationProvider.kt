package net.thunderbird.android.provider

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider

class TbAppIconNotificationProvider : NotificationIconResourceProvider {
    override val pushNotificationIcon: Int
        get() = app.k9mail.core.ui.legacy.theme2.thunderbird.R.drawable.ic_logo_thunderbird_white
}
