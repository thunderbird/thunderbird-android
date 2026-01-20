package app.k9mail.legacy.mailstore

import app.k9mail.legacy.mailstore.FolderTypeMapper.folderTypeOf
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
class FolderRepository(
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getFolder(accountId: AccountId, folderId: Long): Folder? {
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

    suspend fun getFolderDetails(accountId: AccountId, folderId: Long): FolderDetails? {
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
    fun getRemoteFolders(accountId: AccountId): List<RemoteFolder> {
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

    fun getRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails> {
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

    fun getPushFoldersFlow(accountId: AccountId): Flow<List<RemoteFolder>> {
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

    private fun getPushFolders(accountId: AccountId): List<RemoteFolder> {
        return getRemoteFolderDetails(accountId)
            .asSequence()
            .filter { folderDetails -> folderDetails.isPushEnabled }
            .map { folderDetails -> folderDetails.folder }
            .toList()
    }

    fun getFolderServerId(accountId: AccountId, folderId: Long): String? {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolder(folderId) { folder ->
            folder.serverId
        }
    }

    fun getFolderId(accountId: AccountId, folderServerId: String): Long? {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolderId(folderServerId)
    }

    fun isFolderPresent(accountId: AccountId, folderId: Long): Boolean {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getFolder(folderId) { true } ?: false
    }

    fun updateFolderDetails(accountId: AccountId, folderDetails: FolderDetails) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.updateFolderSettings(folderDetails)
    }

    fun setIncludeInUnifiedInbox(accountId: AccountId, folderId: Long, includeInUnifiedInbox: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    fun setVisible(accountId: AccountId, folderId: Long, visible: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setVisible(folderId, visible)
    }

    fun setSyncEnabled(accountId: AccountId, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setSyncEnabled(folderId, enable)
    }

    fun setNotificationsEnabled(accountId: AccountId, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setNotificationsEnabled(folderId, enable)
    }

    fun setPushDisabled(accountId: AccountId) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setPushDisabled()
    }

    fun hasPushEnabledFolder(accountId: AccountId): Boolean {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.hasPushEnabledFolder()
    }

    fun hasPushEnabledFolderFlow(accountId: AccountId): Flow<Boolean> {
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
            folderTypeOf(account, id)
        }

    private suspend fun getAccountById(accountId: AccountId): LegacyAccount =
        accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
}

data class RemoteFolderDetails(
    val folder: RemoteFolder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val isSyncEnabled: Boolean,
    val isVisible: Boolean,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
)
