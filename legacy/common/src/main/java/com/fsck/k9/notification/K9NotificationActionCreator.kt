package com.fsck.k9.notification

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.PendingIntentCompat
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.activity.MainActivity
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import com.fsck.k9.ui.notification.DeleteConfirmationActivity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * This class contains methods to create the [PendingIntent]s for the actions of our notifications.
 *
 * **Note:**
 * We need to take special care to ensure the `PendingIntent`s are unique as defined in the documentation of
 * [PendingIntent]. Otherwise selecting a notification action might perform the action on the wrong message.
 *
 * We add unique values to `Intent.data` so we end up with unique `PendingIntent`s.
 *
 * In the past we've used the notification ID as `requestCode` argument when creating a `PendingIntent`. But since we're
 * reusing notification IDs, it's safer to make sure the `Intent` itself is unique.
 */
internal class K9NotificationActionCreator(
    private val context: Context,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val messageStoreManager: MessageStoreManager,
    private val generalSettingsManager: GeneralSettingsManager,
) : NotificationActionCreator {
    private val interactionSettings get() = generalSettingsManager.getConfig().interaction

    override fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        val openInUnifiedInbox =
            generalSettingsManager.getConfig().display.inboxSettings.isShowUnifiedInbox &&
                isIncludedInUnifiedInbox(messageReference)
        val intent = createMessageViewIntent(messageReference, openInUnifiedInbox)

        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createViewFolderPendingIntent(account: LegacyAccountDto, folderId: Long): PendingIntent {
        val intent = createMessageListIntent(account, folderId)
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createViewMessagesPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent {
        val folderIds = extractFolderIds(messageReferences)

        val intent = if (generalSettingsManager.getConfig()
                .display
                .inboxSettings
                .isShowUnifiedInbox &&
            areAllIncludedInUnifiedInbox(account, folderIds)
        ) {
            createUnifiedInboxIntent(account)
        } else if (folderIds.size == 1) {
            createMessageListIntent(account, folderIds.first())
        } else {
            createNewMessagesIntent(account)
        }
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createViewFolderListPendingIntent(account: LegacyAccountDto): PendingIntent {
        val intent = createMessageListIntent(account)
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createDismissAllMessagesPendingIntent(account: LegacyAccountDto): PendingIntent {
        val intent = NotificationActionService.createDismissAllMessagesIntent(context, account).apply {
            data = Uri.parse("data:,dismissAll/${account.uuid}/${System.currentTimeMillis()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createDismissMessageIntent(context, messageReference).apply {
            data = Uri.parse("data:,dismiss/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = MessageActions.getActionReplyIntent(context, messageReference).apply {
            data = Uri.parse("data:,reply/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference).apply {
            data = Uri.parse("data:,markAsRead/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createMarkAllAsReadPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent =
            NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences).apply {
                data = Uri.parse("data:,markAllAsRead/$accountUuid/${System.currentTimeMillis()}")
            }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun getEditIncomingServerSettingsIntent(account: LegacyAccountDto): PendingIntent {
        val intent = FeatureLauncherActivity.getIntent(
            context = context,
            target = FeatureLauncherTarget.AccountEditIncomingSettings(account.uuid),
        )
        return PendingIntentCompat.getActivity(context, account.accountNumber, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun getEditOutgoingServerSettingsIntent(account: LegacyAccountDto): PendingIntent {
        val intent = FeatureLauncherActivity.getIntent(
            context = context,
            target = FeatureLauncherTarget.AccountEditOutgoingSettings(account.uuid),
        )
        return PendingIntentCompat.getActivity(context, account.accountNumber, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        return if (K9.isConfirmDeleteFromNotification) {
            createDeleteConfirmationPendingIntent(messageReference)
        } else {
            createDeleteServicePendingIntent(messageReference)
        }
    }

    private fun createDeleteServicePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createDeleteMessageIntent(context, messageReference).apply {
            data = Uri.parse("data:,delete/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    private fun createDeleteConfirmationPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReference).apply {
            data = Uri.parse("data:,deleteConfirmation/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createDeleteAllPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent {
        return if (interactionSettings.isConfirmDeleteFromNotification) {
            getDeleteAllConfirmationPendingIntent(messageReferences)
        } else {
            getDeleteAllServicePendingIntent(account, messageReferences)
        }
    }

    private fun getDeleteAllConfirmationPendingIntent(messageReferences: List<MessageReference>): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReferences).apply {
            data = Uri.parse("data:,deleteAllConfirmation/${System.currentTimeMillis()}")
        }
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_CANCEL_CURRENT, false)!!
    }

    private fun getDeleteAllServicePendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent =
            NotificationActionService.createDeleteAllMessagesIntent(context, accountUuid, messageReferences).apply {
                data = Uri.parse("data:,deleteAll/$accountUuid/${System.currentTimeMillis()}")
            }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createArchiveMessageIntent(context, messageReference).apply {
            data = Uri.parse("data:,archive/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createArchiveAllPendingIntent(
        account: LegacyAccountDto,
        messageReferences: List<MessageReference>,
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences).apply {
            data = Uri.parse("data:,archiveAll/${account.uuid}/${System.currentTimeMillis()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    override fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference).apply {
            data = Uri.parse("data:,spam/${messageReference.toIdentityString()}")
        }
        return PendingIntentCompat.getService(context, 0, intent, FLAG_UPDATE_CURRENT, false)!!
    }

    private fun createMessageListIntent(account: LegacyAccountDto): Intent {
        val folderId = defaultFolderProvider.getDefaultFolder(account)
        val search = LocalMessageSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MainActivity.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true,
        ).apply {
            data = Uri.parse("data:,messageList/${account.uuid}/$folderId")
        }
    }

    private fun createMessageListIntent(account: LegacyAccountDto, folderId: Long): Intent {
        val search = LocalMessageSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MainActivity.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true,
        ).apply {
            data = Uri.parse("data:,messageList/${account.uuid}/$folderId")
        }
    }

    private fun createMessageViewIntent(messageReference: MessageReference, openInUnifiedInbox: Boolean): Intent {
        return MainActivity.actionDisplayMessageIntent(context, messageReference, openInUnifiedInbox).apply {
            data = Uri.parse("data:,messageView/${messageReference.toIdentityString()}")
        }
    }

    private fun createUnifiedInboxIntent(account: LegacyAccountDto): Intent {
        return MainActivity.createUnifiedInboxIntent(context, account).apply {
            data = Uri.parse("data:,unifiedInbox/${account.uuid}")
        }
    }

    private fun createNewMessagesIntent(account: LegacyAccountDto): Intent {
        return MainActivity.createNewMessagesIntent(context, account).apply {
            data = Uri.parse("data:,newMessages/${account.uuid}")
        }
    }

    private fun extractFolderIds(messageReferences: List<MessageReference>): Set<Long> {
        return messageReferences.asSequence().map { it.folderId }.toSet()
    }

    private fun areAllIncludedInUnifiedInbox(account: LegacyAccountDto, folderIds: Collection<Long>): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.areAllIncludedInUnifiedInbox(folderIds)
    }

    private fun isIncludedInUnifiedInbox(messageReference: MessageReference): Boolean {
        val messageStore = messageStoreManager.getMessageStore(messageReference.accountUuid)
        return messageStore.areAllIncludedInUnifiedInbox(listOf(messageReference.folderId))
    }
}
