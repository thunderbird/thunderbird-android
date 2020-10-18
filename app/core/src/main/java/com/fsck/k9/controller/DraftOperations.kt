package com.fsck.k9.controller

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingReplace
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import timber.log.Timber

internal class DraftOperations(private val messagingController: MessagingController) {

    fun saveDraft(
        account: Account,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?
    ): Long? {
        return try {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val localStore = messagingController.getLocalStoreOrThrow(account)
            val localFolder = localStore.getFolder(draftsFolderId)
            localFolder.open()

            val localMessage = if (messagingController.supportsUpload(account)) {
                saveAndUploadDraft(account, message, localFolder, existingDraftId)
            } else {
                saveDraftLocally(message, localFolder, existingDraftId)
            }

            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true)
            if (plaintextSubject != null) {
                localMessage.setCachedDecryptedSubject(plaintextSubject)
            }

            localMessage.databaseId
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to save message as draft.")
            null
        }
    }

    private fun saveAndUploadDraft(
        account: Account,
        message: Message,
        localFolder: LocalFolder,
        existingDraftId: Long?
    ): LocalMessage {
        localFolder.appendMessages(listOf(message))

        val localMessage = localFolder.getMessage(message.uid)
        val previousDraftMessage = if (existingDraftId != null) localFolder.getMessage(existingDraftId) else null

        val folderId = localFolder.databaseId
        if (previousDraftMessage != null) {
            previousDraftMessage.delete()

            val uploadMessageId = localMessage.databaseId
            val deleteMessageId = previousDraftMessage.databaseId
            val command = PendingReplace.create(folderId, uploadMessageId, deleteMessageId)
            messagingController.queuePendingCommand(account, command)
        } else {
            val command = PendingAppend.create(folderId, localMessage.uid)
            messagingController.queuePendingCommand(account, command)
        }

        messagingController.processPendingCommands(account)

        return localMessage
    }

    private fun saveDraftLocally(message: Message, localFolder: LocalFolder, existingDraftId: Long?): LocalMessage {
        if (existingDraftId != null) {
            // Setting the UID will cause LocalFolder.appendMessages() to replace the existing draft.
            message.uid = localFolder.getMessageUidById(existingDraftId)
        }

        localFolder.appendMessages(listOf(message))

        return localFolder.getMessage(message.uid)
    }

    fun processPendingReplace(command: PendingReplace, account: Account) {
        val localStore = messagingController.getLocalStoreOrThrow(account)
        val localFolder = localStore.getFolder(command.folderId)
        localFolder.open()

        val backend = messagingController.getBackend(account)

        val uploadMessageId = command.uploadMessageId
        val localMessage = localFolder.getMessage(uploadMessageId)
        if (localMessage == null) {
            Timber.w("Couldn't find local copy of message to upload [ID: %d]", uploadMessageId)
            return
        } else if (!localMessage.uid.startsWith(K9.LOCAL_UID_PREFIX)) {
            Timber.i("Message [ID: %d] to be uploaded already has a server ID set. Skipping upload.", uploadMessageId)
        } else {
            uploadMessage(backend, account, localFolder, localMessage)
        }

        deleteMessage(backend, localFolder, command.deleteMessageId)
    }

    private fun uploadMessage(
        backend: Backend,
        account: Account,
        localFolder: LocalFolder,
        localMessage: LocalMessage
    ) {
        val folderServerId = localFolder.serverId
        Timber.d("Uploading message [ID: %d] to remote folder '%s'", localMessage.databaseId, folderServerId)

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.BODY)
        }
        localFolder.fetch(listOf(localMessage), fetchProfile, null)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            Timber.w(
                "Failed to get a server ID for the uploaded message. Removing local copy [ID: %d]",
                localMessage.databaseId
            )
            localMessage.destroy()
        } else {
            val oldUid = localMessage.uid

            localMessage.uid = messageServerId
            localFolder.changeUid(localMessage)

            for (listener in messagingController.listeners) {
                listener.messageUidChanged(account, localFolder.databaseId, oldUid, localMessage.uid)
            }
        }
    }

    private fun deleteMessage(backend: Backend, localFolder: LocalFolder, messageId: Long) {
        val messageServerId = localFolder.getMessageUidById(messageId) ?: run {
            Timber.i("Couldn't find local copy of message [ID: %d] to be deleted. Skipping delete.", messageId)
            return
        }

        val messageServerIds = listOf(messageServerId)
        val folderServerId = localFolder.serverId
        backend.deleteMessages(folderServerId, messageServerIds)

        messagingController.destroyPlaceholderMessages(localFolder, messageServerIds)
    }
}
