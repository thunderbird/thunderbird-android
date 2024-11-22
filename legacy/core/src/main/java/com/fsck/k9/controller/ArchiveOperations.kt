package com.fsck.k9.controller

import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.FeatureFlagResult
import app.k9mail.core.featureflag.toFeatureFlagKey
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessagingController.MessageActor
import com.fsck.k9.controller.MessagingController.MoveOrCopyFlavor
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import timber.log.Timber

internal class ArchiveOperations(
    private val messagingController: MessagingController,
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
        action: (account: Account, folderId: Long, messagesInFolder: List<LocalMessage>, archiveFolderId: Long) -> Unit,
    ) {
        actOnMessagesGroupedByAccountAndFolder(messages) { account, messageFolder, messagesInFolder ->
            val sourceFolderId = messageFolder.databaseId
            when (val archiveFolderId = account.archiveFolderId) {
                null -> {
                    Timber.v("No archive folder configured for account %s", account)
                }

                sourceFolderId -> {
                    Timber.v("Skipping messages already in archive folder")
                }

                else -> {
                    messagingController.suppressMessages(account, messagesInFolder)
                    messagingController.putBackground(description, null) {
                        action(account, sourceFolderId, messagesInFolder, archiveFolderId)
                    }
                }
            }
        }
    }

    private fun archiveThreads(
        account: Account,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        val messagesInThreads = messagingController.collectMessagesInThreads(account, messages)
        archiveMessages(account, sourceFolderId, messagesInThreads, archiveFolderId)
    }

    private fun archiveMessages(
        account: Account,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        val operation = when (featureFlagProvider.provide("move_and_mark_as_read".toFeatureFlagKey())) {
            FeatureFlagResult.Enabled -> MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ
            FeatureFlagResult.Disabled -> MoveOrCopyFlavor.MOVE
            FeatureFlagResult.Unavailable -> MoveOrCopyFlavor.MOVE
        }
        messagingController.moveOrCopyMessageSynchronous(
            account,
            sourceFolderId,
            messages,
            archiveFolderId,
            operation,
        )
    }

    private fun actOnMessagesGroupedByAccountAndFolder(
        messages: List<MessageReference>,
        block: (account: Account, messageFolder: LocalFolder, messages: List<LocalMessage>) -> Unit,
    ) {
        val actor = MessageActor { account, messageFolder, messagesInFolder ->
            block(account, messageFolder, messagesInFolder)
        }

        messagingController.actOnMessagesGroupedByAccountAndFolder(messages, actor)
    }
}
