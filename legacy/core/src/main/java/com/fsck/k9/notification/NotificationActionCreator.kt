package com.fsck.k9.notification

import android.app.PendingIntent
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference

interface NotificationActionCreator {
    fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createViewFolderPendingIntent(account: Account, folderId: Long): PendingIntent

    fun createViewMessagesPendingIntent(account: Account, messageReferences: List<MessageReference>): PendingIntent

    fun createViewFolderListPendingIntent(account: Account): PendingIntent

    fun createDismissAllMessagesPendingIntent(account: Account): PendingIntent

    fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkAllAsReadPendingIntent(account: Account, messageReferences: List<MessageReference>): PendingIntent

    fun getEditIncomingServerSettingsIntent(account: Account): PendingIntent

    fun getEditOutgoingServerSettingsIntent(account: Account): PendingIntent

    fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createDeleteAllPendingIntent(account: Account, messageReferences: List<MessageReference>): PendingIntent

    fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createArchiveAllPendingIntent(account: Account, messageReferences: List<MessageReference>): PendingIntent

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent
}
