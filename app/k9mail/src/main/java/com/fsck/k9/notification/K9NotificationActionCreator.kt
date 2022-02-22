package com.fsck.k9.notification

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.activity.setup.AccountSetupIncoming
import com.fsck.k9.activity.setup.AccountSetupOutgoing
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE
import com.fsck.k9.mailstore.MessageStoreManager
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import com.fsck.k9.ui.notification.DeleteConfirmationActivity

/**
 * This class contains methods to create the [PendingIntent]s for the actions of our notifications.
 *
 * **Note:**
 * We need to take special care to ensure the `PendingIntent`s are unique as defined in the documentation of
 * [PendingIntent]. Otherwise selecting a notification action might perform the action on the wrong message.
 *
 * We use the notification ID as `requestCode` argument to ensure each notification/action pair gets a unique
 * `PendingIntent`.
 */
internal class K9NotificationActionCreator(
    private val context: Context,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val messageStoreManager: MessageStoreManager
) : NotificationActionCreator {

    override fun createViewMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val openInUnifiedInbox = K9.isShowUnifiedInbox && isIncludedInUnifiedInbox(messageReference)
        val intent = createMessageViewIntent(messageReference, openInUnifiedInbox)

        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createViewFolderPendingIntent(account: Account, folderId: Long, notificationId: Int): PendingIntent {
        val intent = createMessageListIntent(account, folderId)
        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createViewMessagesPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val folderIds = extractFolderIds(messageReferences)

        val intent = if (K9.isShowUnifiedInbox && areAllIncludedInUnifiedInbox(account, folderIds)) {
            createUnifiedInboxIntent(account)
        } else if (folderIds.size == 1) {
            createMessageListIntent(account, folderIds.first())
        } else {
            createNewMessagesIntent(account)
        }

        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createViewFolderListPendingIntent(account: Account, notificationId: Int): PendingIntent {
        val intent = createMessageListIntent(account)
        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createDismissAllMessagesPendingIntent(account: Account, notificationId: Int): PendingIntent {
        val intent = NotificationActionService.createDismissAllMessagesIntent(context, account)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createDismissMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createDismissMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createReplyPendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent {
        val intent = MessageActions.getActionReplyIntent(context, messageReference)
        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createMarkMessageAsReadPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createMarkAllAsReadPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent = NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun getEditIncomingServerSettingsIntent(account: Account): PendingIntent {
        val intent = AccountSetupIncoming.intentActionEditIncomingSettings(context, account)
        return PendingIntent.getActivity(context, account.accountNumber, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun getEditOutgoingServerSettingsIntent(account: Account): PendingIntent {
        val intent = AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account)
        return PendingIntent.getActivity(context, account.accountNumber, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createDeleteMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        return if (K9.isConfirmDeleteFromNotification) {
            createDeleteConfirmationPendingIntent(messageReference, notificationId)
        } else {
            createDeleteServicePendingIntent(messageReference, notificationId)
        }
    }

    private fun createDeleteServicePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createDeleteMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    private fun createDeleteConfirmationPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReference)
        return PendingIntent.getActivity(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createDeleteAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        return if (K9.isConfirmDeleteFromNotification) {
            getDeleteAllConfirmationPendingIntent(messageReferences, notificationId)
        } else {
            getDeleteAllServicePendingIntent(account, messageReferences, notificationId)
        }
    }

    private fun getDeleteAllConfirmationPendingIntent(
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReferences)
        return PendingIntent.getActivity(context, notificationId, intent, FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE)
    }

    private fun getDeleteAllServicePendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent = NotificationActionService.createDeleteAllMessagesIntent(context, accountUuid, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createArchiveMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createArchiveAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    override fun createMarkMessageAsSpamPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    private fun createMessageListIntent(account: Account): Intent {
        val folderId = defaultFolderProvider.getDefaultFolder(account)
        val search = LocalSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MessageList.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true
        )
    }

    private fun createMessageListIntent(account: Account, folderId: Long): Intent {
        val search = LocalSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MessageList.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true
        )
    }

    private fun createMessageViewIntent(message: MessageReference, openInUnifiedInbox: Boolean): Intent {
        return MessageList.actionDisplayMessageIntent(context, message, openInUnifiedInbox)
    }

    private fun createUnifiedInboxIntent(account: Account): Intent {
        return MessageList.createUnifiedInboxIntent(context, account)
    }

    private fun createNewMessagesIntent(account: Account): Intent {
        return MessageList.createNewMessagesIntent(context, account)
    }

    private fun extractFolderIds(messageReferences: List<MessageReference>): Set<Long> {
        return messageReferences.asSequence().map { it.folderId }.toSet()
    }

    private fun areAllIncludedInUnifiedInbox(account: Account, folderIds: Collection<Long>): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.areAllIncludedInUnifiedInbox(folderIds)
    }

    private fun isIncludedInUnifiedInbox(messageReference: MessageReference): Boolean {
        val messageStore = messageStoreManager.getMessageStore(messageReference.accountUuid)
        return messageStore.areAllIncludedInUnifiedInbox(listOf(messageReference.folderId))
    }
}
