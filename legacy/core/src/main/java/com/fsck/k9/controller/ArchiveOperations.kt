package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessagingController.MessageActor
import com.fsck.k9.controller.MessagingController.MoveOrCopyFlavor
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.toFeatureFlagKey
import net.thunderbird.core.logging.legacy.Log

internal class ArchiveOperations(
    private val messagingController: MessagingController,
    private val featureFlagProvider: FeatureFlagProvider,
    private val archiveFolderResolver: ArchiveFolderResolver = ArchiveFolderResolver(),
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
            account: LegacyAccountDto,
            folderId: Long,
            messagesInFolder: List<LocalMessage>,
            archiveFolderId: Long,
        ) -> Unit,
    ) {
        actOnMessagesGroupedByAccountAndFolder(messages) { account, messageFolder, messagesInFolder ->
            val sourceFolderId = messageFolder.databaseId
            when (val archiveFolderId = account.archiveFolderId) {
                null -> {
                    Log.v("No archive folder configured for account %s", account)
                }

                sourceFolderId -> {
                    Log.v("Skipping messages already in archive folder")
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
        account: LegacyAccountDto,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        val messagesInThreads = messagingController.collectMessagesInThreads(account, messages)
        archiveMessages(account, sourceFolderId, messagesInThreads, archiveFolderId)
    }

    private fun archiveMessages(
        account: LegacyAccountDto,
        sourceFolderId: Long,
        messages: List<LocalMessage>,
        archiveFolderId: Long,
    ) {
        // Group messages by their resolved archive destination folder
        // This allows yearly/monthly granularity to route messages to different subfolders
        val messagesByDestination = messages.groupBy { message ->
            archiveFolderResolver.resolveArchiveFolder(account, message) ?: archiveFolderId
        }

        val operation = featureFlagProvider.provide("archive_marks_as_read".toFeatureFlagKey())
            .whenEnabledOrNot(
                onEnabled = { MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ },
                onDisabledOrUnavailable = { MoveOrCopyFlavor.MOVE },
            )

        // Archive each group to its respective destination folder
        for ((destinationFolderId, messagesForFolder) in messagesByDestination) {
            messagingController.moveOrCopyMessageSynchronous(
                account,
                sourceFolderId,
                messagesForFolder,
                destinationFolderId,
                operation,
            )
        }
    }

    private fun actOnMessagesGroupedByAccountAndFolder(
        messages: List<MessageReference>,
        block: (account: LegacyAccountDto, messageFolder: LocalFolder, messages: List<LocalMessage>) -> Unit,
    ) {
        val actor = MessageActor { account, messageFolder, messagesInFolder ->
            block(account, messageFolder, messagesInFolder)
        }

        messagingController.actOnMessagesGroupedByAccountAndFolder(messages, actor)
    }
}
