package app.k9mail.legacy.mailstore

import app.k9mail.legacy.mailstore.RemoteFolderTypeMapper.toFolderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.RemoteFolder

@Suppress("TooManyFunctions")
class DefaultFolderRepository(
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderRepository {
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

    override suspend fun getFolderDetails(accountId: AccountId, folderId: Long): FolderDetails? {
        val account = getAccountById(accountId)
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId)
        return messageStore.getFolder(folderId) { folder ->
            FolderDetails(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folder.getFolderType(account, outboxFolderId),
                    isLocalOnly = folder.isLocalOnly,
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

    override fun getPushFoldersFlow(accountId: AccountId): Flow<List<RemoteFolder>> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return callbackFlow {
            send(getPushFolders(accountId))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(getPushFolders(accountId))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun getPushFolders(accountId: AccountId): List<RemoteFolder> {
        return getRemoteFolderDetails(accountId)
            .asSequence()
            .filter { folderDetails -> folderDetails.isPushEnabled }
            .map { folderDetails -> folderDetails.folder }
            .toList()
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

    override fun updateFolderDetails(accountId: AccountId, folderDetails: FolderDetails) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.updateFolderSettings(folderDetails)
    }

    override fun setIncludeInUnifiedInbox(accountId: AccountId, folderId: Long, includeInUnifiedInbox: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    override fun setVisible(accountId: AccountId, folderId: Long, visible: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setVisible(folderId, visible)
    }

    override fun setSyncEnabled(accountId: AccountId, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setSyncEnabled(folderId, enable)
    }

    override fun setNotificationsEnabled(accountId: AccountId, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setNotificationsEnabled(folderId, enable)
    }

    override fun setPushDisabled(accountId: AccountId) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setPushDisabled()
    }

    override fun hasPushEnabledFolder(accountId: AccountId): Boolean {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.hasPushEnabledFolder()
    }

    override fun hasPushEnabledFolderFlow(accountId: AccountId): Flow<Boolean> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return callbackFlow {
            send(hasPushEnabledFolder(accountId))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(hasPushEnabledFolder(accountId))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private fun FolderDetailsAccessor.getFolderType(account: LegacyAccount, outboxFolderId: Long): FolderType =
        if (id == outboxFolderId) {
            FolderType.OUTBOX
        } else {
            FolderTypeMapper.folderTypeOf(account, id)
        }

    private suspend fun getAccountById(accountId: AccountId): LegacyAccount =
        accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
}
