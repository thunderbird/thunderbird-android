package com.fsck.k9.notification

import android.app.PendingIntent
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference

interface NotificationActionCreator {
    fun createViewMessagePendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent

    fun createViewFolderPendingIntent(account: Account, folderId: Long, notificationId: Int): PendingIntent

    fun createViewMessagesPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent

    fun createViewFolderListPendingIntent(account: Account, notificationId: Int): PendingIntent

    fun createDismissAllMessagesPendingIntent(account: Account, notificationId: Int): PendingIntent

    fun createDismissMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent

    fun createReplyPendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent

    fun createMarkAllAsReadPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent

    fun getEditIncomingServerSettingsIntent(account: Account): PendingIntent

    fun getEditOutgoingServerSettingsIntent(account: Account): PendingIntent

    fun createDeleteMessagePendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent

    fun createDeleteAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent

    fun createArchiveMessagePendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent

    fun createArchiveAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent
}
