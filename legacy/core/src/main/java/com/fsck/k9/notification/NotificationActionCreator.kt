package com.fsck.k9.notification

import android.app.PendingIntent
import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.core.android.account.LegacyAccountDto

interface NotificationActionCreator {
    fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createViewFolderPendingIntent(account: LegacyAccountDto, folderId: Long): PendingIntent

    fun createViewMessagesPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createViewFolderListPendingIntent(account: LegacyAccountDto): PendingIntent

    fun createDismissAllMessagesPendingIntent(account: LegacyAccountDto): PendingIntent

    fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkAllAsReadPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun getEditIncomingServerSettingsIntent(account: LegacyAccountDto): PendingIntent

    fun getEditOutgoingServerSettingsIntent(account: LegacyAccountDto): PendingIntent

    fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createDeleteAllPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent

    fun createArchiveAllPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent

    fun createMarkMessageAsStarPendingIntent(messageReference: MessageReference): PendingIntent
}
