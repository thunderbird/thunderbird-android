package app.k9mail.legacy.mailstore.folder.push

import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository

private const val LOG_ID = "[repository][push-folder-tracking]"

class DefaultPushFolderTrackingRepository(
    private val logger: Logger,
    private val messageStoreManager: MessageStoreManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PushFolderTrackingRepository {
    override fun observeEnabled(accountId: AccountId): Flow<Outcome<Boolean, FolderError>> = callbackFlow {
        logger.verbose { "$LOG_ID starting observing push enabled for account '$accountId'" }
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val enabled = isEnabled(accountId, messageStore)
        logger.verbose { "$LOG_ID push enabled = '$enabled' for account id '$accountId'" }
        send(enabled)

        val listener = FolderSettingsChangedListener {
            withContext(ioDispatcher) {
                trySendBlocking(isEnabled(accountId, messageStore))
            }
        }
        messageStore.addFolderSettingsChangedListener(listener)

        awaitClose {
            logger.verbose { "$LOG_ID stop observing push enabled for account '$accountId'" }
            messageStore.removeFolderSettingsChangedListener(listener)
        }
    }
        .buffer(capacity = Channel.CONFLATED)
        .distinctUntilChanged()
        .catch { throwable ->
            logger.error(throwable = throwable) {
                "$LOG_ID Failed to observe push enabled for account id: $accountId"
            }
            when (throwable) {
                is IllegalStateException -> emit(Outcome.failure(FolderError.AccountNotFound))
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun isEnabled(accountId: AccountId): Outcome<Boolean, FolderError> {
        return try {
            isEnabled(accountId, messageStoreManager.getMessageStore(accountId))
        } catch (e: IllegalStateException) {
            logger.error(throwable = e) {
                "$LOG_ID Failed to disable push for account id: $accountId"
            }
            Outcome.failure(FolderError.AccountNotFound)
        }
    }

    override suspend fun disable(accountId: AccountId): Outcome<Unit, FolderError> = try {
        logger.verbose { "$LOG_ID disabling push enabled for account '$accountId'" }
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setPushDisabled()
        logger.verbose { "$LOG_ID push disabled for account '$accountId'" }
        Outcome.success()
    } catch (e: IllegalStateException) {
        logger.error(throwable = e) {
            "$LOG_ID Failed to disable push for account id: $accountId"
        }
        Outcome.failure(FolderError.AccountNotFound)
    }

    private fun isEnabled(
        accountId: AccountId,
        messageStore: MessageStore = messageStoreManager.getMessageStore(accountId),
    ): Outcome<Boolean, FolderError> = Outcome.success(messageStore.hasPushEnabledFolder())
}
