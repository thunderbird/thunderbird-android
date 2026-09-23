package net.thunderbird.app.common.feature.mail.message.data.repository

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.mailstore.LocalStoreProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageQueryError
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository

private const val LOG_ID = "[repository][message-query]"

class DefaultMessageQueryRepository(
    private val logger: Logger,
    private val accountManager: LegacyAccountManager,
    private val localStoreProvider: LocalStoreProvider,
    private val messageStoreManager: MessageStoreManager,
    private val messageIdLegacyEntityIdFactory: LegacyEntityIdFactory<MessageId>,
    private val folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessageQueryRepository {
    override suspend fun findIdByCriteria(
        accountId: AccountId,
        criteria: GetMessageIdCriteria,
    ): Outcome<MessageId?, MessageQueryError> = withContext(ioDispatcher) {
        logger.verbose { "$LOG_ID finding message id by account id = '$accountId' and criteria = $criteria" }
        val account = accountManager.getAccount(accountId.toString())
            ?: return@withContext accountNotFound(accountId)

        runLegacy(operation = "find message id by $criteria") {
            val lockableDatabase = localStoreProvider.getInstanceByLegacyAccount(account).database
            val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(criteria.folderId)
            val rawServerId = criteria.messageServerId.value
            val legacyMessageId = lockableDatabase.execute(false) { db ->
                db.query(
                    "messages",
                    arrayOf("id"),
                    "folder_id = ? AND uid = ?",
                    arrayOf(legacyFolderId.toString(), rawServerId),
                    null,
                    null,
                    null,
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
            }
            legacyMessageId?.let(messageIdLegacyEntityIdFactory::of).also { messageId ->
                logger.verbose { "$LOG_ID message id = '$messageId'" }
            }
        }
    }

    override suspend fun getAllServerIdByFolderId(
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Set<MessageServerId>, MessageQueryError> = withContext(ioDispatcher) {
        logger.verbose { "$LOG_ID getting message server ids by folder id '$folderId' and account id = '$accountId'" }
        accountManager.getAccount(accountId.toString())
            ?: return@withContext accountNotFound(accountId)

        runLegacy(operation = "get server ids of folder '$folderId'") {
            val messageStore = messageStoreManager.getMessageStore(accountId)
            val legacyFolderId = folderIdLegacyEntityIdFactory.toLegacyId(folderId)
            messageStore.getMessageServerIds(legacyFolderId)
                .map(::MessageServerId)
                .toSet()
                .also { serverIds -> logger.verbose { "$LOG_ID found ${serverIds.size} server ids" } }
        }
    }

    private fun <T> accountNotFound(accountId: AccountId): Outcome<T, MessageQueryError> {
        logger.warn { "$LOG_ID account '$accountId' not found" }
        return Outcome.failure(MessageQueryError.AccountNotFound(accountId))
    }

    /**
     * Runs a legacy store operation and converts any thrown exception into a [MessageQueryError.UnhandledError].
     * [CancellationException] is rethrown so structured concurrency keeps working.
     */
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runLegacy(
        operation: String,
        block: () -> T,
    ): Outcome<T, MessageQueryError> = try {
        Outcome.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(throwable = e) { "$LOG_ID $operation failed" }
        Outcome.failure(MessageQueryError.UnhandledError(e))
    }
}
