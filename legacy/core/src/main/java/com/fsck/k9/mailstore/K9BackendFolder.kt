package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageStore
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.mail.MessageDownloadState
import java.util.Date
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.core.outcome.handle
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.domain.MessageLifecycleError
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import app.k9mail.legacy.mailstore.MoreMessages as StoreMoreMessages
import com.fsck.k9.mail.Message as LegacyMessage
import net.thunderbird.feature.mail.message.MessageDownloadState as DomainMessageDownloadState

class K9BackendFolder(
    private val logger: Logger,
    private val messageStore: MessageStore,
    folderServerId: String,
    private val messageLifecycleRepository: MessageLifecycleRepository,
    private val folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    private val mapper: MessageDataMapper<LegacyMessage>,
) : BackendFolder {
    private val databaseId: String
    private val folderId: Long
    override val name: String
    override val visibleLimit: Int

    init {
        data class Init(val folderId: Long, val name: String, val visibleLimit: Int)

        val init = messageStore.getFolder(folderServerId) { folder ->
            Init(
                folderId = folder.id,
                name = folder.name,
                visibleLimit = folder.visibleLimit,
            )
        } ?: error("Couldn't find folder $folderServerId")

        databaseId = init.folderId.toString()
        folderId = init.folderId
        name = init.name
        visibleLimit = init.visibleLimit
    }

    override fun getMessageServerIds(): Set<String> {
        return messageStore.getMessageServerIds(folderId)
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        return messageStore.getAllMessagesAndEffectiveDates(folderId)
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun clearAllMessages() {
        val messageServerIds = messageStore.getMessageServerIds(folderId)
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun getMoreMessages(): MoreMessages {
        return messageStore.getFolder(folderId) { folder ->
            folder.moreMessages.toMoreMessages()
        } ?: MoreMessages.UNKNOWN
    }

    override fun setMoreMessages(moreMessages: MoreMessages) {
        messageStore.setMoreMessages(folderId, moreMessages.toStoreMoreMessages())
    }

    override fun setLastChecked(timestamp: Long) {
        messageStore.setLastChecked(folderId, timestamp)
    }

    override fun setStatus(status: String?) {
        messageStore.setStatus(folderId, status)
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        return messageStore.isMessagePresent(folderId, messageServerId)
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        return messageStore.getMessageFlags(folderId, messageServerId)
    }

    override fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean) {
        messageStore.setMessageFlag(folderId, messageServerId, flag, value)
    }

    override suspend fun saveMessage(message: LegacyMessage, downloadState: MessageDownloadState) {
        requireMessageServerId(message)
        val accountId = messageStore.accountId
        message.setAccountUuid(accountId.toString())
        val domainFolderId = folderIdLegacyEntityIdFactory.of(folderId)
        // A message coming from the backend carries no X_DOWNLOADED_* flags yet, so the mapper would
        // infer ENVELOPE. The backend tells us explicitly how much was downloaded; that must win.
        val domainMessage = mapper.toDomain(message).copy(downloadState = downloadState.toDomainDownloadState())

        messageLifecycleRepository
            .create(domainMessage, accountId, domainFolderId)
            .recoverAlreadyExists { existingId ->
                // The message is already stored locally (e.g. re-download with a more complete body),
                // so replace it instead.
                messageLifecycleRepository.update(
                    message = domainMessage.copy(id = existingId),
                    accountId = accountId,
                    folderId = domainFolderId,
                )
            }
            .orThrow("Failed to save message '${message.uid}'")
    }

    private suspend fun Outcome<MessageId, MessageLifecycleError>.recoverAlreadyExists(
        recover: suspend (existingId: MessageId) -> Outcome<MessageId, MessageLifecycleError>,
    ): Outcome<MessageId, MessageLifecycleError> {
        val error = (this as? Outcome.Failure)?.error
        return when (this) {
            is Outcome.Success -> this
            is Outcome.Failure if error is MessageLifecycleError.MessageAlreadyExists -> recover(error.messageId)
            else -> this
        }
    }

    private fun Outcome<MessageId, MessageLifecycleError>.orThrow(message: String) {
        handle(
            onSuccess = { messageId -> logger.verbose { "Applied changes to message id '$messageId'" } },
            onFailure = { error ->
                throw error.throwable ?: MessagingException("$message: $error")
            },
        )
    }

    override fun getOldestMessageDate(): Date? {
        return messageStore.getOldestMessageDate(folderId)
    }

    override fun getFolderExtraString(name: String): String? {
        return messageStore.getFolderExtraString(folderId, name)
    }

    override fun setFolderExtraString(name: String, value: String?) {
        messageStore.setFolderExtraString(folderId, name, value)
    }

    override fun getFolderExtraNumber(name: String): Long? {
        return messageStore.getFolderExtraNumber(folderId, name)
    }

    override fun setFolderExtraNumber(name: String, value: Long) {
        messageStore.setFolderExtraNumber(folderId, name, value)
    }

    private fun StoreMoreMessages.toMoreMessages(): MoreMessages = when (this) {
        StoreMoreMessages.UNKNOWN -> MoreMessages.UNKNOWN
        StoreMoreMessages.FALSE -> MoreMessages.FALSE
        StoreMoreMessages.TRUE -> MoreMessages.TRUE
    }

    private fun MoreMessages.toStoreMoreMessages(): StoreMoreMessages = when (this) {
        MoreMessages.UNKNOWN -> StoreMoreMessages.UNKNOWN
        MoreMessages.FALSE -> StoreMoreMessages.FALSE
        MoreMessages.TRUE -> StoreMoreMessages.TRUE
    }

    private fun MessageDownloadState.toDomainDownloadState(): DomainMessageDownloadState = when (this) {
        MessageDownloadState.ENVELOPE -> DomainMessageDownloadState.ENVELOPE
        MessageDownloadState.PARTIAL -> DomainMessageDownloadState.PARTIAL
        MessageDownloadState.FULL -> DomainMessageDownloadState.FULL
    }

    private fun requireMessageServerId(message: LegacyMessage) {
        if (message.uid.isNullOrEmpty()) {
            error("Message requires a server ID to be set")
        }
    }
}
