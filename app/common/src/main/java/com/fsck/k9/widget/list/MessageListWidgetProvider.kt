package com.fsck.k9.widget.list

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig

class MessageListWidgetProvider : app.k9mail.feature.widget.message.list.MessageListWidgetProvider()

internal class K9MessageListWidgetConfig : MessageListWidgetConfig {
    override val providerClass = MessageListWidgetProvider::class.java
}
