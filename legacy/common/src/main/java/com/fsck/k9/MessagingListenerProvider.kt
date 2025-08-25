package com.fsck.k9

import app.k9mail.legacy.message.controller.MessagingListener

interface MessagingListenerProvider {
    val listeners: List<MessagingListener>
}

class DefaultMessagingListenerProvider(
    override val listeners: List<MessagingListener>,
) : MessagingListenerProvider
