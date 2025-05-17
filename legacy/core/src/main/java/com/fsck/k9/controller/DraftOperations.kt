package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.SaveMessageData
import com.fsck.k9.K9
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingReplace
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.SaveMessageDataCreator
import net.thunderbird.core.android.account.LegacyAccount
import org.jetbrains.annotations.NotNull
import timber.log.Timber

internal class DraftOperations(
    private val messagingController: @NotNull MessagingController,
    private val messageStoreManager: @NotNull MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator,
) {

    fun saveDraft(
        account: LegacyAccount,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long? {
        return try {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val messageId = if (messagingController.supportsUpload(account)) {
                saveAndUploadDraft(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            } else {
                saveDraftLocally(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            }

            messageId
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to save message as draft.")
            null
        }
    }

    private fun saveAndUploadDraft(
        account: LegacyAccount,
        message: Message,
        folderId: Long,
        existingDraftId: Long?,
        subject: String?,
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)

        val messageId = messageStore.saveLocalMessage(folderId, message.toSaveMessageData(subject))

        val previousDraftMessage = existingDraftId?.let {
            val localStore = messagingController.getLocalStoreOrThrow(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            localFolder.getMessage(existingDraftId)
        }

        if (previousDraftMessage != null) {
            previousDraftMessage.delete()

            val deleteMessageId = previousDraftMessage.databaseId
            val command = PendingReplace.create(folderId, messageId, deleteMessageId)
            messagingController.queuePendingCommand(account, command)
        } else {
            val fakeMessageServerId = messageStore.getMessageServerId(messageId)
            if (fakeMessageServerId != null) {
                val command = PendingAppend.create(folderId, fakeMessageServerId)
                messagingController.queuePendingCommand(account, command)
            }
        }

        messagingController.processPendingCommands(account)

        return messageId
    }

    private fun saveDraftLocally(
        account: LegacyAccount,
        message: Message,
        folderId: Long,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)
        val messageData = message.toSaveMessageData(plaintextSubject)

        return messageStore.saveLocalMessage(folderId, messageData, existingDraftId)
    }

    fun processPendingReplace(command: PendingReplace, account: LegacyAccount) {
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
        account: LegacyAccount,
        localFolder: LocalFolder,
        localMessage: LocalMessage,
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
                localMessage.databaseId,
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

    private fun Message.toSaveMessageData(subject: String?): SaveMessageData {
        return saveMessageDataCreator.createSaveMessageData(this, MessageDownloadState.FULL, subject)
    }
}
