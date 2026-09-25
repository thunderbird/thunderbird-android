package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.SaveMessageData
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingReplace
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.SaveMessageDataCreator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.jetbrains.annotations.NotNull
import com.fsck.k9.mail.Message as LegacyMessage

internal class DraftOperations @JvmOverloads constructor(
    private val logger: Logger,
    private val messagingController: @NotNull MessagingController,
    private val messageStoreManager: @NotNull MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    private val localMessageUidPrefixProvider: LocalMessageUidPrefixProvider,
    private val messageLifecycleRepository: MessageLifecycleRepository,
    private val messageDataMapper: MessageDataMapper<LegacyMessage>,
    private val messageIdFactory: LegacyEntityIdFactory<MessageId>,
    private val folderIdFactory: LegacyEntityIdFactory<FolderId>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    fun saveDraft(
        account: LegacyAccountDto,
        message: LegacyMessage,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long? = runBlocking(ioDispatcher) {
        try {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val messageId = if (messagingController.supportsUpload(account)) {
                saveAndUploadDraft(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            } else {
                saveDraftLocally(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            }

            messageId
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Unable to save message as draft." }
            null
        }
    }

    private suspend fun saveAndUploadDraft(
        account: LegacyAccountDto,
        message: LegacyMessage,
        folderId: Long,
        existingDraftId: Long?,
        subject: String?,
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)
        val domainMessage = message.toDomainMessage(subject, account)
        val domainFolderId = folderIdFactory.of(folderId)
        val outcome = messageLifecycleRepository.create(domainMessage, account.id, domainFolderId)
        val messageId = outcome.fold(
            onSuccess = messageIdFactory::toLegacyId,
            onFailure = { error("Failed to create draft local message") },
        )

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
        account: LegacyAccountDto,
        message: LegacyMessage,
        folderId: Long,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)
        val messageData = message.toSaveMessageData(plaintextSubject)

        return messageStore.saveLocalMessage(folderId, messageData, existingDraftId)
    }

    fun processPendingReplace(command: PendingReplace, account: LegacyAccountDto) {
        val localStore = messagingController.getLocalStoreOrThrow(account)
        val localFolder = localStore.getFolder(command.folderId)
        localFolder.open()

        val backend = messagingController.getBackend(account)

        val uploadMessageId = command.uploadMessageId
        val localMessage = localFolder.getMessage(uploadMessageId)
        if (localMessage == null) {
            logger.warn { "Couldn't find local copy of message to upload [ID: $uploadMessageId]" }
            return
        } else if (!localMessage.uid.startsWith(localMessageUidPrefixProvider.get())) {
            logger.info {
                "Message [ID: $uploadMessageId] to be uploaded already has a server ID set. Skipping upload."
            }
        } else {
            uploadMessage(backend, account, localFolder, localMessage)
        }

        deleteMessage(backend, localFolder, command.deleteMessageId)
    }

    private fun uploadMessage(
        backend: Backend,
        account: LegacyAccountDto,
        localFolder: LocalFolder,
        localMessage: LocalMessage,
    ) {
        val folderServerId = localFolder.serverId
        logger.debug { "Uploading message [ID: ${localMessage.databaseId}] to remote folder '$folderServerId'" }

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.BODY)
        }
        localFolder.fetch(listOf(localMessage), fetchProfile, null)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            logger.warn {
                "Failed to get a server ID for the uploaded message. Removing local copy " +
                    "[ID: ${localMessage.databaseId}]"
            }
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
            logger.info { "Couldn't find local copy of message [ID: $messageId] to be deleted. Skipping delete." }
            return
        }

        val messageServerIds = listOf(messageServerId)
        val folderServerId = localFolder.serverId
        backend.deleteMessages(folderServerId, messageServerIds)

        messagingController.destroyPlaceholderMessages(localFolder, messageServerIds)
    }

    private fun LegacyMessage.toSaveMessageData(subject: String?): SaveMessageData {
        return saveMessageDataCreator.createSaveMessageData(this, MessageDownloadState.FULL, subject)
    }

    private suspend fun LegacyMessage.toDomainMessage(subject: String?, account: LegacyAccountDto): Message {
        setFlag(Flag.X_DOWNLOADED_FULL, true)
        setAccountUuid(account.uuid)
        val domainMessage = messageDataMapper.toDomain(this).let { domainMessage ->
            if (subject.isNullOrBlank()) {
                domainMessage
            } else {
                domainMessage.copy(envelope = domainMessage.envelope.copy(subject = subject))
            }
        }
        return domainMessage
    }
}
