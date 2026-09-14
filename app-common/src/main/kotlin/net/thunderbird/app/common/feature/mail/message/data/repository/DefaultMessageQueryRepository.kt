package net.thunderbird.app.common.feature.mail.message.data.repository

import com.fsck.k9.mailstore.LocalStoreProvider
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
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageQueryError
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository

private const val LOG_ID = "[repository][message-query]"
class DefaultMessageQueryRepository(
    private val logger: Logger,
    private val accountManager: LegacyAccountManager,
    private val localStoreProvider: LocalStoreProvider,
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
            ?: error("account not found")
        val localStore = localStoreProvider.getInstanceByLegacyAccount(account)
        val lockableDatabase = localStore.database
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
                if (cursor.moveToFirst()) {
                    val messageId = cursor.getLong(0)
                    messageId
                } else {
                    null
                }
            }
        }
        val messageId = legacyMessageId?.let(messageIdLegacyEntityIdFactory::of)
        logger.verbose { "message id = '$messageId'" }
        Outcome.success(messageId)
    }
}
