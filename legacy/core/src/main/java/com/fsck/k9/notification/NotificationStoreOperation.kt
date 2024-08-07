package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference

sealed interface NotificationStoreOperation {
    data class Add(
        val messageReference: MessageReference,
        val notificationId: Int,
        val timestamp: Long,
    ) : NotificationStoreOperation

    data class Remove(val messageReference: MessageReference) : NotificationStoreOperation

    data class ChangeToInactive(val messageReference: MessageReference) : NotificationStoreOperation

    data class ChangeToActive(
        val messageReference: MessageReference,
        val notificationId: Int,
    ) : NotificationStoreOperation
}
