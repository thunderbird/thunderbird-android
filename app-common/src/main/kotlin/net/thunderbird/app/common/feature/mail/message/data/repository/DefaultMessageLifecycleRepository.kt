package net.thunderbird.app.common.feature.mail.message.data.repository

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.SaveMessageData
import com.fsck.k9.mailstore.SaveMessageDataCreator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.app.common.feature.mail.message.domain.model.LegacyMessageSource
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageDownloadState
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.MessageLifecycleError
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import com.fsck.k9.mail.Message as LegacyMessageDto
import com.fsck.k9.mail.MessageDownloadState as LegacyMessageDownloadState

private const val LOG_ID = "[repository][message-lifecycle]"

class DefaultMessageLifecycleRepository(
    private val logger: Logger,
    private val messageStoreManager: MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    private val messageMapper: MessageDataMapper<LegacyMessageDto>,
    private val messageIdLegacyEntityIdFactory: LegacyEntityIdFactory<MessageId>,
    private val folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessageLifecycleRepository {
    override suspend fun create(
        message: Message,
        accountId: AccountId,
        folderId: FolderId?,
    ): Outcome<MessageId, MessageLifecycleError> = withContext(ioDispatcher) {
        logger.debug {
            "$LOG_ID creating new message in folder '${folderId ?: "<outbox-id>"}' for account '$accountId'"
        }
        logger.verbose { "$LOG_ID message = $message" }
        val messageData = message.toSaveMessageData()
        val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
        val legacyFolderId = if (folderId == null) {
            outboxFolderManager.getOutboxFolderId(accountId)
        } else {
            folderIdLegacyEntityIdFactory.toLegacyId(folderId)
        }
        logger.verbose { "$LOG_ID assigning message to folder id = $legacyFolderId" }

        val messageServerId = message.serverId
        val messageId = if (messageServerId == null) {
            createLocalMessage(messageStore, legacyFolderId, messageData)
        } else {
            createRemoteMessage(messageStore, legacyFolderId, messageServerId, messageData)
        }
        logger.verbose { "$LOG_ID message created successfully. MessageId = $messageId" }
        Outcome.success(messageId)
    }

    override suspend fun update(
        message: Message,
        accountId: AccountId,
    ): Outcome<MessageId, MessageLifecycleError> {
        TODO("Not yet implemented")
    }

    override suspend fun move(
        messageId: MessageId,
        destinationFolderId: FolderId,
    ): Outcome<MessageId, MessageLifecycleError> {
        TODO("Not yet implemented")
    }

    override suspend fun copy(
        messageId: MessageId,
        destinationFolderId: FolderId,
    ): Outcome<MessageId, MessageLifecycleError> {
        TODO("Not yet implemented")
    }

    override suspend fun destroy(
        serverIds: List<MessageServerId>,
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Unit, MessageLifecycleError> {
        TODO("Not yet implemented")
    }

    private fun createLocalMessage(
        messageStore: ListenableMessageStore,
        legacyFolderId: Long,
        messageData: SaveMessageData,
    ): MessageId {
        logger.verbose { "$LOG_ID creating local message in folder id '$legacyFolderId'" }
        val legacyMessageId = messageStore.saveLocalMessage(
            folderId = legacyFolderId,
            messageData = messageData,
            existingMessageId = null,
        )
        return messageIdLegacyEntityIdFactory.of(legacyMessageId)
    }

    private fun createRemoteMessage(
        messageStore: ListenableMessageStore,
        legacyFolderId: Long,
        messageServerId: MessageServerId,
        messageData: SaveMessageData,
    ): MessageId {
        logger.verbose { "$LOG_ID creating remote message in folder id '$legacyFolderId'" }
        val legacyMessageId = messageStore.saveRemoteMessage(
            folderId = legacyFolderId,
            messageData = messageData,
            messageServerId = messageServerId.value,
        )
        return messageIdLegacyEntityIdFactory.of(legacyMessageId)
    }

    private fun MessageDownloadState.toLegacyDownloadState(): LegacyMessageDownloadState = when (this) {
        MessageDownloadState.ENVELOPE -> LegacyMessageDownloadState.ENVELOPE
        MessageDownloadState.PARTIAL -> LegacyMessageDownloadState.PARTIAL
        MessageDownloadState.FULL -> LegacyMessageDownloadState.FULL
    }

    private suspend fun Message.toSaveMessageData(): SaveMessageData {
        val downloadState = downloadState.toLegacyDownloadState()
        return when (val source = source) {
            is LegacyMessageSource -> {
                logger.verbose {
                    "$LOG_ID convert message -> save message data using message source ($source)"
                }
                saveMessageDataCreator.createSaveMessageData(
                    source.message,
                    downloadState,
                    envelope.subject,
                )
            }

            else -> {
                logger.verbose { "$LOG_ID convert message -> save message data using message mapper" }
                saveMessageDataCreator.createSaveMessageData(messageMapper.toDto(this), downloadState)
            }
        }
    }
}
