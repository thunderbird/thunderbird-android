package com.fsck.k9.controller

import com.fsck.k9.Account
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import timber.log.Timber

internal class DraftOperations(private val messagingController: MessagingController) {

    fun saveDraft(
        account: Account,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?
    ): Message? {
        return try {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val localStore = messagingController.getLocalStoreOrThrow(account)
            val localFolder = localStore.getFolder(draftsFolderId)
            localFolder.open()

            if (existingDraftId != null) {
                val uid = localFolder.getMessageUidById(existingDraftId)
                message.uid = uid
            }

            // Save the message to the store.
            localFolder.appendMessages(listOf(message))

            // Fetch the message back from the store.  This is the Message that's returned to the caller.
            val localMessage = localFolder.getMessage(message.uid)
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true)
            if (plaintextSubject != null) {
                localMessage.setCachedDecryptedSubject(plaintextSubject)
            }

            if (messagingController.supportsUpload(account)) {
                val command = PendingAppend.create(localFolder.databaseId, localMessage.uid)
                messagingController.queuePendingCommand(account, command)
                messagingController.processPendingCommands(account)
            }

            localMessage
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to save message as draft.")
            null
        }
    }
}
