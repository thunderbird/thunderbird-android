package app.k9mail.provider

import android.content.Context
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.legacy.theme2.k9mail.R

class K9AppNotificationIconProvider(
    private val context: Context,

) : NotificationIconResourceProvider {
    override val pushNotificationIcon: Int

        get() = R.drawable.ic_logo_k9_white
}
