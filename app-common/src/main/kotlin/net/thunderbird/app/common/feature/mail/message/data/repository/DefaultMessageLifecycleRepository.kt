package net.thunderbird.app.common.feature.mail.message.data.repository

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.SaveMessageData
import com.fsck.k9.mailstore.SaveMessageDataCreator
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.app.common.feature.mail.message.domain.model.LegacyMessageSource
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageDownloadState
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageLifecycleError
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import com.fsck.k9.mail.Message as LegacyMessageDto
import com.fsck.k9.mail.MessageDownloadState as LegacyMessageDownloadState

private const val LOG_ID = "[repository][message-lifecycle]"

@Suppress("TooManyFunctions")
class DefaultMessageLifecycleRepository(
    private val logger: Logger,
    private val messageQueryRepository: MessageQueryRepository,
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
        val messageServerId = message.serverId
        val existingMessageId = verifyMessageExists(folderId, messageServerId, accountId)
        if (existingMessageId != null) {
            logger.warn { "$LOG_ID message already exists with id '$existingMessageId'. Use update instead." }
            return@withContext Outcome.failure(MessageLifecycleError.MessageAlreadyExists(existingMessageId))
        }
        logger.debug {
            "$LOG_ID creating new message in folder '${folderId ?: "<outbox-id>"}' for account '$accountId'"
        }
        logger.verbose { "$LOG_ID message = $message" }
        val messageData = message.toSaveMessageData(
            logger = logger,
            saveMessageDataCreator = saveMessageDataCreator,
            messageDataMapper = messageMapper,
        )
        val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
        val legacyFolderId = if (folderId == null) {
            outboxFolderManager.getOutboxFolderId(accountId)
        } else {
            folderIdLegacyEntityIdFactory.toLegacyId(folderId)
        }
        logger.verbose { "$LOG_ID assigning message to folder id = $legacyFolderId" }

        val messageId = if (messageServerId == null) {
            upsertLocalMessage(messageStore, legacyFolderId, messageData)
        } else {
            upsertRemoteMessage(messageStore, legacyFolderId, messageServerId, messageData)
        }
        logger.verbose { "$LOG_ID message created successfully. MessageId = $messageId" }
        Outcome.success(messageId)
    }

    override suspend fun update(
        message: Message,
        accountId: AccountId,
        folderId: FolderId,
    ): Outcome<MessageId, MessageLifecycleError> = withContext(ioDispatcher) {
        // NOTE: The legacy store resolves remote messages by folder id, server id (messages.uid), so
        //       message.id is not strictly needed for that branch today. We still require it so the
        //       update() contract is the same for local and remote messages and stays valid when
        //       the data source is replaced.
        //       Revisit upsertRemoteMessage once the new db can update by message id directly.
        val messageId = requireNotNull(message.id) { "$LOG_ID The message.id is required to update a message." }
        logger.debug {
            "$LOG_ID updating message '$messageId' in folder '$folderId' for account '$accountId'"
        }
        logger.verbose { "$LOG_ID message = $message" }
        val legacyMessage = messageMapper.toDto(message)
        val downloadState = message.downloadState.toLegacyDownloadState()
        val messageData = saveMessageDataCreator.createSaveMessageData(legacyMessage, downloadState)
        val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
        val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(folderId)
        val messageServerId = message.serverId
        val updatedMessageId = if (messageServerId == null) {
            val legacyExistingMessageId = messageIdLegacyEntityIdFactory.toLegacyId(messageId)
            upsertLocalMessage(messageStore, legacyFolderId, messageData, legacyExistingMessageId)
        } else {
            upsertRemoteMessage(messageStore, legacyFolderId, messageServerId, messageData)
        }
        logger.verbose { "$LOG_ID message updated successfully. MessageId = $updatedMessageId" }
        Outcome.success(updatedMessageId)
    }

    override suspend fun move(
        messageId: MessageId,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<MessageId, MessageLifecycleError> = withContext(ioDispatcher) {
        logger.debug { "$LOG_ID moving '$messageId' to folder '$destinationFolderId' for account '$accountId'" }
        val legacyMessageId = messageIdLegacyEntityIdFactory.toLegacyId(messageId)
        val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(destinationFolderId)
        logger.verbose {
            "$LOG_ID message id '$messageId' -> legacy id '$legacyMessageId', " +
                "folder id '$destinationFolderId' -> legacy id '$legacyFolderId'"
        }

        runLegacy(operation = "move message '$messageId'") {
            val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
            val legacyDestinationMessageId = messageStore.moveMessage(legacyMessageId, legacyFolderId)
            messageIdLegacyEntityIdFactory.of(legacyDestinationMessageId).also { destinationMessageId ->
                logger.verbose { "$LOG_ID moved message '$messageId' -> '$destinationMessageId'" }
            }
        }
    }

    override suspend fun moveAll(
        messageIds: List<MessageId>,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<Map<MessageId, MessageId>, MessageLifecycleError> = withContext(ioDispatcher) {
        if (messageIds.isEmpty()) {
            logger.verbose { "$LOG_ID nothing to move to folder '$destinationFolderId' for account '$accountId'" }
            return@withContext Outcome.success(emptyMap())
        }
        logger.debug {
            "$LOG_ID moving ${messageIds.size} messages to folder '$destinationFolderId' for account '$accountId'"
        }
        val legacyMessageIds = messageIds.map(messageIdLegacyEntityIdFactory::toLegacyId)
        val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(destinationFolderId)
        logger.verbose {
            "$LOG_ID message ids $messageIds -> legacy ids $legacyMessageIds, " +
                "folder id '$destinationFolderId' -> legacy id '$legacyFolderId'"
        }

        runLegacy(operation = "move ${messageIds.size} messages") {
            val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
            messageStore.moveMessages(legacyMessageIds, legacyFolderId)
                .entries
                .associate { (sourceLegacyId, destinationLegacyId) ->
                    messageIdLegacyEntityIdFactory.of(sourceLegacyId) to
                        messageIdLegacyEntityIdFactory.of(destinationLegacyId)
                }
                .also { mapping ->
                    logger.verbose {
                        val lines = mapping.entries.joinToString("\n") { (source, destination) ->
                            "'$source' -> '$destination'"
                        }
                        "$LOG_ID moved messages to folder '$destinationFolderId':\n${lines.prependIndent("  ")}"
                    }
                }
        }
    }

    override suspend fun copy(
        messageId: MessageId,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<MessageId, MessageLifecycleError> = withContext(ioDispatcher) {
        logger.debug { "$LOG_ID copying '$messageId' to folder '$destinationFolderId' for account '$accountId'" }
        val legacyMessageId = messageIdLegacyEntityIdFactory.toLegacyId(messageId)
        val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(destinationFolderId)
        logger.verbose {
            "$LOG_ID message id '$messageId' -> legacy id '$legacyMessageId', " +
                "folder id '$destinationFolderId' -> legacy id '$legacyFolderId'"
        }

        runLegacy(operation = "copy message '$messageId'") {
            val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
            val legacyDestinationMessageId = messageStore.copyMessage(legacyMessageId, legacyFolderId)
            messageIdLegacyEntityIdFactory.of(legacyDestinationMessageId).also { destinationMessageId ->
                logger.verbose { "$LOG_ID copied message '$messageId' -> '$destinationMessageId'" }
            }
        }
    }

    override suspend fun copyAll(
        messageIds: List<MessageId>,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<Map<MessageId, MessageId>, MessageLifecycleError> = withContext(ioDispatcher) {
        if (messageIds.isEmpty()) {
            logger.verbose { "$LOG_ID nothing to copy to folder '$destinationFolderId' for account '$accountId'" }
            return@withContext Outcome.success(emptyMap())
        }
        logger.debug {
            "$LOG_ID copying ${messageIds.size} messages to folder '$destinationFolderId' for account '$accountId'"
        }
        val legacyMessageIds = messageIds.map(messageIdLegacyEntityIdFactory::toLegacyId)
        val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(destinationFolderId)
        logger.verbose {
            "$LOG_ID message ids $messageIds -> legacy ids $legacyMessageIds, " +
                "folder id '$destinationFolderId' -> legacy id '$legacyFolderId'"
        }
        runLegacy(operation = "copy ${messageIds.size} messages") {
            val messageStore = messageStoreManager.getMessageStore(accountId.value.toString())
            messageStore.copyMessages(legacyMessageIds, legacyFolderId)
                .entries
                .associate { (sourceLegacyId, destinationLegacyId) ->
                    messageIdLegacyEntityIdFactory.of(sourceLegacyId) to
                        messageIdLegacyEntityIdFactory.of(destinationLegacyId)
                }
                .also { mapping ->
                    logger.verbose {
                        val lines = mapping.entries.joinToString("\n") { (source, destination) ->
                            "'$source' -> '$destination'"
                        }
                        "$LOG_ID copied messages to folder '$destinationFolderId':\n${lines.prependIndent("  ")}"
                    }
                }
        }
    }

    override suspend fun destroy(
        serverIds: List<MessageServerId>,
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Unit, MessageLifecycleError> {
        TODO("Not yet implemented")
    }

    /**
     * Runs a legacy store operation and converts any thrown exception into a [MessageLifecycleError.UnhandledError].
     * [CancellationException] is rethrown so structured concurrency keeps working.
     */
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runLegacy(
        operation: String,
        block: () -> T,
    ): Outcome<T, MessageLifecycleError> = try {
        Outcome.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(throwable = e) { "$LOG_ID $operation failed" }
        Outcome.failure(MessageLifecycleError.UnhandledError(e))
    }

    private suspend fun verifyMessageExists(
        folderId: FolderId?,
        messageServerId: MessageServerId?,
        accountId: AccountId,
    ): MessageId? {
        return if (folderId != null && messageServerId != null) {
            messageQueryRepository
                .findIdByCriteria(accountId, GetMessageIdCriteria(folderId, messageServerId))
                .fold(onSuccess = { it }, onFailure = { throw it.throwable })
        } else {
            null
        }
    }

    private fun upsertLocalMessage(
        messageStore: ListenableMessageStore,
        legacyFolderId: Long,
        messageData: SaveMessageData,
        existingLegacyMessageId: Long? = null,
    ): MessageId {
        logger.verbose { "$LOG_ID creating local message in folder id '$legacyFolderId'" }
        val legacyMessageId = messageStore.saveLocalMessage(
            folderId = legacyFolderId,
            messageData = messageData,
            existingMessageId = existingLegacyMessageId,
        )
        return messageIdLegacyEntityIdFactory.of(legacyMessageId)
    }

    private fun upsertRemoteMessage(
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
}

private fun MessageDownloadState.toLegacyDownloadState(): LegacyMessageDownloadState = when (this) {
    MessageDownloadState.ENVELOPE -> LegacyMessageDownloadState.ENVELOPE
    MessageDownloadState.PARTIAL -> LegacyMessageDownloadState.PARTIAL
    MessageDownloadState.FULL -> LegacyMessageDownloadState.FULL
}

private suspend fun Message.toSaveMessageData(
    logger: Logger,
    saveMessageDataCreator: SaveMessageDataCreator,
    messageDataMapper: MessageDataMapper<LegacyMessageDto>,
): SaveMessageData {
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
            saveMessageDataCreator.createSaveMessageData(messageDataMapper.toDto(this), downloadState)
        }
    }
}
