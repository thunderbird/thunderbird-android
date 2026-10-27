package app.k9mail.legacy.mailstore.folder.push

import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.MessageStoreManager
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
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.logging.Logger
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

        return remoteFolderDetailsRepository.getAllByAccountId(accountId).fold(
            onSuccess = { folderDetails ->
                val pushFolders = folderDetails
                    .filter { it.isPushEnabled }
                    .map { it.folder }

                if (pushFolders.isEmpty()) {
                    Outcome.failure(FolderError.NotFound)
                } else {
                    Outcome.success(pushFolders)
                }
            },
            onFailure = { error -> Outcome.failure(error) },
        )
    }

    companion object {
        private const val LOG_ID = "[repository][push-folders-query]"
    }
}
