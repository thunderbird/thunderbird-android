package app.k9mail.widget

import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import com.fsck.k9.provider.UnreadWidgetProvider

class K9UnreadWidgetConfig : UnreadWidgetConfig {
    override val providerClass = UnreadWidgetProvider::class.java
}
