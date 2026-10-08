package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessagingController.MessageActor
import com.fsck.k9.controller.MessagingController.MoveOrCopyFlavor
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log

internal class ArchiveOperations(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountManager,
    private val featureFlagProvider: FeatureFlagProvider,
) {
    fun archiveThreads(messages: List<MessageReference>) {
        archiveByFolder("archiveThreads", messages) { account, folderId, messagesInFolder, archiveFolderId ->
            archiveThreads(account, folderId, messagesInFolder, archiveFolderId)
        }
    }

    fun archiveMessages(messages: List<MessageReference>) {
        archiveByFolder("archiveMessages", messages) { account, folderId, messagesInFolder, archiveFolderId ->
            archiveMessages(account, folderId, messagesInFolder, archiveFolderId)
        }
    }

    fun archiveMessage(message: MessageReference) {
        archiveMessages(listOf(message))
    }

    private fun archiveByFolder(
        description: String,
        messages: List<MessageReference>,
        action: (
            accountId: AccountId,
            folderId: Long,
            messagesInFolder: List<LocalMessage>,
            archiveFolderId: Long,
        ) -> Unit,
    ) {
        actOnMessagesGroupedByAccountAndFolder(messages) { accountId, messageFolder, messagesInFolder ->
            val sourceFolderId = messageFolder.databaseId
            val account = accountManager.findById(accountId) ?: return@actOnMessagesGroupedByAccountAndFolder
            when (val archiveFolderId = account.archiveFolderId) {
                null -> {
                    Log.v("No archive folder configured for account %s", accountId)
                }

                sourceFolderId -> {
                    Log.v("Skipping messages already in archive folder")
                }

                else -> {
                    messagingController.suppressMessages(accountId, messagesInFolder)
                    messagingController.putBackground(description, null) {
                        action(accountId, sourceFolderId, messagesInFolder, archiveFolderId)
                    }
                }
            }
        }
    }

    private fun archiveThreads(
        accountId: AccountId,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        val messagesInThreads = messagingController.collectMessagesInThreads(accountId, messages)
        archiveMessages(accountId, sourceFolderId, messagesInThreads, archiveFolderId)
    }

    private fun archiveMessages(
        accountId: AccountId,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        val operation = featureFlagProvider.provide(GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ)
            .whenEnabledOrNot(
                onEnabled = { MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ },
                onDisabledOrUnavailable = { MoveOrCopyFlavor.MOVE },
            )
        messagingController.moveOrCopyMessageSynchronous(
            accountId,
            sourceFolderId,
            messages,
            archiveFolderId,
            operation,
        )
    }

    private fun actOnMessagesGroupedByAccountAndFolder(
        messages: List<MessageReference>,
        block: (accountId: AccountId, messageFolder: LocalFolder, messages: List<LocalMessage>) -> Unit,
    ) {
        val actor = MessageActor { accountId, messageFolder, messagesInFolder ->
            block(accountId, messageFolder, messagesInFolder)
        }

        messagingController.actOnMessagesGroupedByAccountAndFolder(messages, actor)
    }
}
