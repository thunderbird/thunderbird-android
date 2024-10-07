package app.k9mail.legacy.mailstore

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderDetails
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.folder.RemoteFolder
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
import kotlinx.coroutines.flow.flowOn

@Suppress("TooManyFunctions")
class FolderRepository(
    private val messageStoreManager: MessageStoreManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun getFolder(account: Account, folderId: Long): Folder? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            Folder(
                id = folder.id,
                name = folder.name,
                type = folderTypeOf(account, folder.id),
                isLocalOnly = folder.isLocalOnly,
            )
        }
    }

    fun getFolderDetails(account: Account, folderId: Long): FolderDetails? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            FolderDetails(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folderTypeOf(account, folder.id),
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

    fun getRemoteFolders(account: Account): List<RemoteFolder> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
            RemoteFolder(
                id = folder.id,
                serverId = folder.serverIdOrThrow(),
                name = folder.name,
                type = folder.type.toFolderType(),
            )
        }
    }

    fun getRemoteFolderDetails(account: Account): List<RemoteFolderDetails> {
        val messageStore = messageStoreManager.getMessageStore(account)
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

    fun getPushFoldersFlow(account: Account): Flow<List<RemoteFolder>> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return callbackFlow {
            send(getPushFolders(account))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(getPushFolders(account))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private fun getPushFolders(account: Account): List<RemoteFolder> {
        return getRemoteFolderDetails(account)
            .asSequence()
            .filter { folderDetails -> folderDetails.isPushEnabled }
            .map { folderDetails -> folderDetails.folder }
            .toList()
    }

    fun getFolderServerId(account: Account, folderId: Long): String? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            folder.serverId
        }
    }

    fun getFolderId(account: Account, folderServerId: String): Long? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolderId(folderServerId)
    }

    fun isFolderPresent(account: Account, folderId: Long): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { true } ?: false
    }

    fun updateFolderDetails(account: Account, folderDetails: FolderDetails) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.updateFolderSettings(folderDetails)
    }

    fun setIncludeInUnifiedInbox(account: Account, folderId: Long, includeInUnifiedInbox: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    fun setVisible(account: Account, folderId: Long, visible: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setVisible(folderId, visible)
    }

    fun setSyncEnabled(account: Account, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setSyncEnabled(folderId, enable)
    }

    fun setNotificationsEnabled(account: Account, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setNotificationsEnabled(folderId, enable)
    }

    fun setPushDisabled(account: Account) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setPushDisabled()
    }

    fun hasPushEnabledFolder(account: Account): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.hasPushEnabledFolder()
    }

    fun hasPushEnabledFolderFlow(account: Account): Flow<Boolean> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return callbackFlow {
            send(hasPushEnabledFolder(account))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(hasPushEnabledFolder(account))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }
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
