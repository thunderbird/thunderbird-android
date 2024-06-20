package net.thunderbird.android.widget

import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import net.thunderbird.android.widget.provider.UnreadWidgetProvider

class TbUnreadWidgetConfig : UnreadWidgetConfig {
    override val providerClass = UnreadWidgetProvider::class.java
}
