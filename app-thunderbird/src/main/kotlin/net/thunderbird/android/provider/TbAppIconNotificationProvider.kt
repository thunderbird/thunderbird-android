package net.thunderbird.android.provider

import android.content.Context
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.legacy.theme2.k9mail.R

class TbAppIconNotificationProvider(context: Context) : NotificationIconResourceProvider {

    override val pushNotificationIcon: Int
        get() = app.k9mail.core.ui.legacy.theme2.thunderbird.R.drawable.ic_logo_thunderbird_white
}
