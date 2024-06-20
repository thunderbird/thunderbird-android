package app.k9mail.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import com.fsck.k9.widget.list.MessageListWidgetProvider

class K9MessageListWidgetConfig : MessageListWidgetConfig {
    override val providerClass = MessageListWidgetProvider::class.java
}
