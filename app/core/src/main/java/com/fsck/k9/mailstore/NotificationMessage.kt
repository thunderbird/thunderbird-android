package com.fsck.k9.mailstore

data class NotificationMessage(
    val message: LocalMessage,
    val notificationId: Int?,
    val timestamp: Long,
)
