package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingReplace
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.account.AccountId
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.jetbrains.annotations.NotNull
import com.fsck.k9.mail.Message as LegacyMessage

private const val TAG = "DraftOperations"

internal class DraftOperations @JvmOverloads constructor(
    private val logger: Logger,
    private val messagingController: @NotNull MessagingController,
    private val messageStoreManager: @NotNull MessageStoreManager,
    private val localMessageUidPrefixProvider: LocalMessageUidPrefixProvider,
    private val messageLifecycleRepository: MessageLifecycleRepository,
    private val messageDataMapper: MessageDataMapper<LegacyMessage>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val accountManager: LegacyAccountManager,
) {

    fun saveDraft(
        accountId: AccountId,
        message: LegacyMessage,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long? = runBlocking(ioDispatcher) {
        try {
            val account = accountManager.findById(accountId) ?: return@runBlocking null
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val messageId = if (messagingController.supportsUpload(accountId)) {
                saveAndUploadDraft(accountId, message, draftsFolderId, existingDraftId, plaintextSubject)
            } else {
                saveDraftLocally(accountId, message, draftsFolderId, existingDraftId, plaintextSubject)
            }

            messageId
        } catch (e: MessagingException) {
            logger.error(tag = TAG, throwable = e) { "Unable to save message as draft." }
            null
        }
    }

    private suspend fun saveAndUploadDraft(
        accountId: AccountId,
        message: LegacyMessage,
        folderId: Long,
        existingDraftId: Long?,
        subject: String?,
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val domainMessage = message.toDomainMessage(subject, accountId, folderId)
        val domainFolderId = LegacyFolderIdFactory.of(folderId)
        val outcome = messageLifecycleRepository.create(domainMessage, accountId, domainFolderId)
        val messageId = outcome.fold(
            onSuccess = LegacyMessageIdFactory::toLegacyId,
            onFailure = { error("Failed to create draft local message") },
        )

        val previousDraftMessage = existingDraftId?.let {
            val localStore = messagingController.getLocalStoreOrThrow(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            localFolder.getMessage(existingDraftId)
        }

        if (previousDraftMessage != null) {
            previousDraftMessage.delete()

            val deleteMessageId = previousDraftMessage.databaseId
            val command = PendingReplace.create(folderId, messageId, deleteMessageId)
            messagingController.queuePendingCommand(accountId, command)
        } else {
            val fakeMessageServerId = messageStore.getMessageServerId(messageId)
            if (fakeMessageServerId != null) {
                val command = PendingAppend.create(folderId, fakeMessageServerId)
                messagingController.queuePendingCommand(accountId, command)
            }
        }

        messagingController.processPendingCommands(accountId)

        return messageId
    }

    private suspend fun saveDraftLocally(
        accountId: AccountId,
        message: LegacyMessage,
        folderId: Long,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long {
        val domainMessage = message.toDomainMessage(plaintextSubject, accountId, folderId)
        val domainFolderId = LegacyFolderIdFactory.of(folderId)
        val accountId = accountId
        return if (existingDraftId == null) {
            messageLifecycleRepository.create(domainMessage, accountId, domainFolderId)
        } else {
            messageLifecycleRepository.update(
                message = domainMessage.copy(id = LegacyMessageIdFactory.of(existingDraftId)),
                accountId = accountId,
                folderId = domainFolderId,
            )
        }.fold(
            onSuccess = LegacyMessageIdFactory::toLegacyId,
            onFailure = { error ->
                throw error.throwable ?: MessagingException("Failed to save draft locally: $error")
            },
        )
    }

    fun processPendingReplace(command: PendingReplace, accountId: AccountId) {
        val localStore = messagingController.getLocalStoreOrThrow(accountId)
        val localFolder = localStore.getFolder(command.folderId)
        localFolder.open()

        val backend = messagingController.getBackend(accountId)

        val uploadMessageId = command.uploadMessageId
        val localMessage = localFolder.getMessage(uploadMessageId)
        if (localMessage == null) {
            logger.warn(TAG) { "Couldn't find local copy of message to upload [ID: $uploadMessageId]" }
            return
        } else if (!localMessage.uid.startsWith(localMessageUidPrefixProvider.get())) {
            logger.info(TAG) {
                "Message [ID: $uploadMessageId] to be uploaded already has a server ID set. Skipping upload."
            }
        } else {
            uploadMessage(backend, accountId, localFolder, localMessage)
        }

        deleteMessage(backend, localFolder, command.deleteMessageId)
    }

    private fun uploadMessage(
        backend: Backend,
        accountId: AccountId,
        localFolder: LocalFolder,
        localMessage: LocalMessage,
    ) {
        val folderServerId = localFolder.serverId
        logger.debug(TAG) { "Uploading message [ID: ${localMessage.databaseId}] to remote folder '$folderServerId'" }

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.BODY)
        }
        localFolder.fetch(listOf(localMessage), fetchProfile, null)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            logger.warn(TAG) {
                "Failed to get a server ID for the uploaded message. Removing local copy " +
                    "[ID: ${localMessage.databaseId}]"
            }
            localMessage.destroy()
        } else {
            val oldUid = localMessage.uid

            localMessage.uid = messageServerId
            localFolder.changeUid(localMessage)

            for (listener in messagingController.listeners) {
                listener.messageUidChanged(accountId, localFolder.databaseId, oldUid, localMessage.uid)
            }
        }
    }

    private fun deleteMessage(backend: Backend, localFolder: LocalFolder, messageId: Long) {
        val messageServerId = localFolder.getMessageUidById(messageId) ?: run {
            logger.info(TAG) { "Couldn't find local copy of message [ID: $messageId] to be deleted. Skipping delete." }
            return
        }

        val messageServerIds = listOf(messageServerId)
        val folderServerId = localFolder.serverId
        backend.deleteMessages(folderServerId, messageServerIds)

        messagingController.destroyPlaceholderMessages(localFolder, messageServerIds)
    }

    private suspend fun LegacyMessage.toDomainMessage(subject: String?, accountId: AccountId, folderId: Long): Message {
        setFlag(Flag.X_DOWNLOADED_FULL, true)
        this.accountId = accountId
        val domainMessage = messageDataMapper.toDomain(this)
        val envelop = if (subject.isNullOrBlank()) {
            domainMessage.envelope
        } else {
            domainMessage.envelope.copy(subject = subject)
        }
        return domainMessage.copy(
            folderId = LegacyFolderIdFactory.of(folderId),
            envelope = envelop,
        )
    }
}
