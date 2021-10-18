package com.fsck.k9.notification

import com.fsck.k9.controller.MessageReference

internal class NotificationContent(
    val messageReference: MessageReference,
    val sender: String,
    val subject: String,
    val preview: CharSequence,
    val summary: CharSequence
)
