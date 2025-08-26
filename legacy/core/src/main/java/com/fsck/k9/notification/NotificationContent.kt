package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mail.Address

internal data class NotificationContent(
    val messageReference: MessageReference,
    val sender: Address,
    val subject: String,
    val preview: CharSequence,
    val summary: CharSequence,
)
