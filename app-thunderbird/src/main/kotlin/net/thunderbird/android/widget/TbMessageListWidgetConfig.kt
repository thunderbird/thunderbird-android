package net.thunderbird.android.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import net.thunderbird.android.widget.provider.MessageListWidgetProvider

class TbMessageListWidgetConfig : MessageListWidgetConfig {
    override val providerClass = MessageListWidgetProvider::class.java
}
