package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference

internal data class NotificationContent(
    val messageReference: MessageReference,
    val sender: String,
    val subject: String,
    val preview: CharSequence,
    val summary: CharSequence,
)
