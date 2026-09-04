package app.k9mail.legacy.mailstore.folder

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.RemoteFolderTypeMapper.toFolderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.RemoteFolderDetails
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderDetailsRepository
import net.thunderbird.feature.mail.folder.api.toStringPiiSafe

private const val LOG_ID = "[repository][remote-folder-details]"

class DefaultRemoteFolderDetailsRepository(
    private val logger: Logger,
    private val messageStoreManager: MessageStoreManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RemoteFolderDetailsRepository {
    override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolderDetails>, FolderError> =
        withContext(ioDispatcher) {
            logger.verbose { "$LOG_ID getting all remote folders details for account '$accountId'" }
            try {
                val messageStore = messageStoreManager.getMessageStore(accountId)
                val folderDetails = messageStore.getFolders(excludeLocalOnly = true) { folder ->
                    RemoteFolderDetails(
                        folder = RemoteFolder(
                            id = folder.id,
                            serverId = folder.serverIdOrThrow(),
                            name = folder.name,
                            type = folder.type.toFolderType(),
                        ),
                        isInTopGroup = folder.isInTopGroup,
                        isIntegrate = folder.isIntegrate,
                        isSyncEnabled = folder.isSyncEnabled,
                        isVisible = folder.isVisible,
                        isNotificationsEnabled = folder.isNotificationsEnabled,
                        isPushEnabled = folder.isPushEnabled,
                    )
                }
                logger.verbose {
                    "$LOG_ID remote folder details = ${folderDetails.joinToString() { it.toStringPiiSafe() }}"
                }
                Outcome.success(folderDetails)
            } catch (e: MessagingException) {
                logger.error(throwable = e) { "$LOG_ID Failed to get remote folders details for account '$accountId'" }

                Outcome.failure(FolderError.FailedToQueryDatabase(message = "Failed to query database.", throwable = e))
            } catch (e: IllegalStateException) {
                logger.error(throwable = e) { "$LOG_ID Failed to get remote folders details for account '$accountId'" }
                Outcome.failure(FolderError.AccountNotFound)
            }
        }
}
