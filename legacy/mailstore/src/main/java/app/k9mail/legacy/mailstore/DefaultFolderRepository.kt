package app.k9mail.legacy.mailstore

import app.k9mail.legacy.mailstore.RemoteFolderTypeMapper.toFolderType
import app.k9mail.legacy.mailstore.folder.extension.getFolderType
import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository

@Suppress("TooManyFunctions")
class DefaultFolderRepository(
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val aggregateRepositories: AggregateRepositories,
) : FolderRepository, PushFolderTrackingRepository by aggregateRepositories.pushFolderTrackingRepository {
    override suspend fun getFolder(accountId: AccountId, folderId: Long): Folder? {
        val account = getAccountById(accountId)
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId)
        return messageStore.getFolder(folderId) { folder ->
            Folder(
                id = folder.id,
                name = folder.name,
                type = folder.getFolderType(account, outboxFolderId),
                isLocalOnly = folder.isLocalOnly,
            )
        }
    }

    @Throws(MessagingException::class)
    override fun getRemoteFolders(accountId: AccountId): List<RemoteFolder> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
            RemoteFolder(
                id = folder.id,
                serverId = folder.serverIdOrThrow(),
                name = folder.name,
                type = folder.type.toFolderType(),
            )
        }
    }

    override fun getRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
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
    }

    override fun getFolderServerId(accountId: AccountId, folderId: Long): String? {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolder(folderId) { folder ->
            folder.serverId
        }
    }

    override fun getFolderId(accountId: AccountId, folderServerId: String): Long? {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolderId(folderServerId)
    }

    override fun isFolderPresent(accountId: AccountId, folderId: Long): Boolean {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolder(folderId) { true } ?: false
    }

    private suspend fun getAccountById(accountId: AccountId): LegacyAccount =
        accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
}

class AggregateRepositories(
    val pushFolderTrackingRepository: PushFolderTrackingRepository,
)
