package app.k9mail.provider

import app.k9mail.core.ui.legacy.theme2.k9mail.R
import net.thunderbird.core.android.common.provider.NotificationIconResourceProvider

class K9AppNotificationIconProvider : NotificationIconResourceProvider {
    override val pushNotificationIcon: Int

        get() = R.drawable.ic_logo_k9_white
}
