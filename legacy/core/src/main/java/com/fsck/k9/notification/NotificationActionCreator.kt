package com.fsck.k9.notification

import android.app.PendingIntent
import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId

interface NotificationActionCreator {
    fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createViewFolderPendingIntent(accountId: AccountId, folderId: Long): PendingIntent

    fun createViewMessagesPendingIntent(
        accountId: AccountId,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createViewFolderListPendingIntent(accountId: AccountId): PendingIntent

    fun createDismissAllMessagesPendingIntent(accountId: AccountId): PendingIntent

    fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkAllAsReadPendingIntent(
        accountId: AccountId,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun getEditIncomingServerSettingsIntent(account: LegacyAccount): PendingIntent

    fun getEditOutgoingServerSettingsIntent(account: LegacyAccount): PendingIntent

    fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createDeleteAllPendingIntent(
        accountId: AccountId,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createArchiveAllPendingIntent(
        accountId: AccountId,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsStarPendingIntent(messageReference: MessageReference): PendingIntent
}
