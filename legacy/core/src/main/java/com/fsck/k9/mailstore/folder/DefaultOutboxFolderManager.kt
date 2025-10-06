package com.fsck.k9.mailstore.folder

import android.os.OperationCanceledException
import app.k9mail.legacy.mailstore.MoreMessages
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.toDatabaseFolderType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.cache.TimeLimitedCache
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.handleAsync
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

private const val TAG = "DefaultOutboxFolderManager"
private const val OUTBOX_FOLDER_NAME = "Outbox"
private const val VISIBLE_LIMIT = 100

class DefaultOutboxFolderManager(
    private val logger: Logger,
    private val accountManager: AccountManager<LegacyAccount>,
    private val localStoreProvider: LocalStoreProvider,
    private val outboxFolderIdCache: TimeLimitedCache<AccountId, Long>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OutboxFolderManager {
    @OptIn(ExperimentalTime::class)
    override suspend fun getOutboxFolderId(
        accountId: AccountId,
        createIfMissing: Boolean,
    ): Long {
        logger.verbose(TAG) { "getOutboxFolderId() called with: uuid = $accountId" }
        outboxFolderIdCache[accountId]?.let { entry ->
            logger.debug(TAG) {
                "getOutboxFolderId: Found Outbox folder with id = ${entry.value} in cache. " +
                    "Cache expires on ${entry.expiresAt}"
            }
            return entry.value
        }

        return withContext(ioDispatcher) {
            val localStore = createLocalStore(accountId)

            var outboxId = try {
                suspendCancellableCoroutine { continuation ->
                    localStore.database.execute(false) { db ->
                        db.rawQuery(
                            "SELECT id FROM folders WHERE type = ?",
                            arrayOf(FolderType.OUTBOX.toDatabaseFolderType()),
                        ).use { cursor ->
                            var id = -1L
                            if (cursor.moveToFirst()) {
                                id = cursor.getLong(0)
                                logger.debug(TAG) { "getOutboxFolderId: Found Outbox folder with id = $id." }
                            }

                            if (id != -1L) {
                                outboxFolderIdCache.set(key = accountId, value = id)
                                continuation.resume(id)
                            } else {
                                continuation.resumeWithException(MessagingException("Outbox folder not found"))
                            }
                        }
                    }
                }
            } catch (e: MessagingException) {
                logger.warn(TAG, e) { "getOutboxFolderId: Couldn't find Outbox folder." }
                -1L
            }

            if (createIfMissing && outboxId == -1L) {
                logger.debug(TAG) { "Creating Outbox folder." }
                createOutboxFolder(accountId).handleAsync(
                    onSuccess = {
                        logger.debug(TAG) { "Created Outbox folder with id = $it." }
                        outboxFolderIdCache.set(key = accountId, value = it)
                        outboxId = it
                    },
                    onFailure = { exception ->
                        logger.error(TAG, exception) { "Failed to create Outbox folder." }
                        throw exception
                    },
                )
            }

            outboxId
        }
    }

    override suspend fun createOutboxFolder(accountId: AccountId): Outcome<Long, Exception> =
        withContext(ioDispatcher) {
            logger.verbose(TAG) { "createOutboxFolder() called with: id = $accountId" }
            val localStore = createLocalStore(accountId)
            try {
                val newId = localStore.createLocalFolder(
                    OUTBOX_FOLDER_NAME,
                    FolderType.OUTBOX,
                    VISIBLE_LIMIT,
                    MoreMessages.UNKNOWN,
                )
                Outcome.Success(newId)
            } catch (e: MessagingException) {
                Outcome.Failure(e)
            }
        }

    override suspend fun hasPendingMessages(accountId: AccountId): Boolean = withContext(ioDispatcher) {
        logger.verbose(TAG) { "hasPendingMessages() called with: id = $accountId" }
        var hasPendingMessages = false
        val localStore = createLocalStore(accountId)
        try {
            localStore.database.execute(false) { db ->
                val query = """
                        |SELECT COUNT(1) FROM messages
                        |WHERE
                        |    empty = 0
                        |    AND deleted = 0
                        |    AND folder_id = (
                        |        SELECT id FROM folders WHERE
                        |            folders.type = ?
                        |            AND folders.local_only = 1
                        |    )
                """.trimMargin()
                db.rawQuery(
                    query,
                    arrayOf(FolderType.OUTBOX.toDatabaseFolderType()),
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        hasPendingMessages = cursor.getInt(0) > 0
                    }
                }
            }
        } catch (e: MessagingException) {
            logger.warn(TAG, e) { "hasPendingMessages: Couldn't check for pending messages." }
        } catch (e: OperationCanceledException) {
            logger.warn(TAG, e) { "hasPendingMessages: Couldn't check for pending messages." }
        }

        hasPendingMessages
    }

    private fun createLocalStore(accountId: AccountId): LocalStore {
        val account = requireNotNull(accountManager.getAccount(accountId.asRaw())) {
            "Account with id $accountId not found"
        }

        return localStoreProvider.getInstance(account = account)
    }
}
