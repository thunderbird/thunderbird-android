package com.fsck.k9.notification

import android.app.PendingIntent
import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.core.android.account.LegacyAccount

interface NotificationActionCreator {
    fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createViewFolderPendingIntent(account: LegacyAccount, folderId: Long): PendingIntent

    fun createViewMessagesPendingIntent(
        account: LegacyAccount,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createViewFolderListPendingIntent(account: LegacyAccount): PendingIntent

    fun createDismissAllMessagesPendingIntent(account: LegacyAccount): PendingIntent

    fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkAllAsReadPendingIntent(
        account: LegacyAccount,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun getEditIncomingServerSettingsIntent(account: LegacyAccount): PendingIntent

    fun getEditOutgoingServerSettingsIntent(account: LegacyAccount): PendingIntent

    fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createDeleteAllPendingIntent(account: LegacyAccount, messageReferences: List<MessageReference>): PendingIntent

    fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createArchiveAllPendingIntent(account: LegacyAccount, messageReferences: List<MessageReference>): PendingIntent

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent
}
