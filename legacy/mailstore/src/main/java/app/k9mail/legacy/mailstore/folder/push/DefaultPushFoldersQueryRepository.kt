package app.k9mail.legacy.mailstore.folder.push

import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.RemoteFolderDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.fold
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.PushFoldersQueryRepository
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderDetailsRepository

class DefaultPushFoldersQueryRepository(
    private val logger: Logger,
    private val messageStoreManager: MessageStoreManager,
    private val remoteFolderDetailsRepository: RemoteFolderDetailsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PushFoldersQueryRepository {
    override fun observeAllByAccountId(accountId: AccountId): Flow<Outcome<List<RemoteFolder>, FolderError>> =
        callbackFlow {
            val messageStore = messageStoreManager.getMessageStore(accountId)

            send(getAllByAccountId(accountId))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(getAllByAccountId(accountId))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }

        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolder>, FolderError> {
        logger.verbose { "$LOG_ID getting push folders for account '$accountId'" }
        val pushFolders = getAllRemoteFolderDetails(accountId)
            .asSequence()
            .filter { folderDetails -> folderDetails.isPushEnabled }
            .map { folderDetails -> folderDetails.folder }
            .toList()

        return if (pushFolders.isEmpty()) {
            logger.warn { "$LOG_ID could not find any push folders with the given account id '$accountId'" }
            Outcome.failure(FolderError.NotFound)
        } else {
            logger.verbose { "$LOG_ID found push folder: $pushFolders" }
            Outcome.success(pushFolders)
        }
    }

    private suspend fun getAllRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails> {
        logger.verbose { "$LOG_ID fetching remote folders details" }
        val outcome = remoteFolderDetailsRepository.getAllByAccountId(accountId)
        return outcome.fold(
            onSuccess = { it },
            onFailure = { error ->
                when (error) {
                    is FolderError.FailedToQueryDatabase -> {
                        logger.error(throwable = error.throwable) {
                            "$LOG_ID Failed to get remote folders details for account '$accountId'"
                        }
                        throw error.throwable
                    }

                    else -> emptyList()
                }
            },
        )
    }

    companion object {
        private const val LOG_ID = "[repository][push-folders-query]"
    }
}
