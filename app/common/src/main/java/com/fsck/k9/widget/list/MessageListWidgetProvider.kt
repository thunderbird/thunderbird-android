package com.fsck.k9.widget.list

import app.k9mail.ui.widget.list.MessageListWidgetConfig

class MessageListWidgetProvider : app.k9mail.ui.widget.list.MessageListWidgetProvider()

internal class K9MessageListWidgetConfig : MessageListWidgetConfig {
    override val providerClass = MessageListWidgetProvider::class.java
}
