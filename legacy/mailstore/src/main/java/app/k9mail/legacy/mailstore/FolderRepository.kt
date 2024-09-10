package app.k9mail.legacy.mailstore

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderDetails
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.FolderMode
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.folder.RemoteFolder
import app.k9mail.legacy.mailstore.FolderTypeMapper.folderTypeOf
import app.k9mail.legacy.mailstore.RemoteFolderTypeMapper.toFolderType
import com.fsck.k9.mail.FolderClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepository(
    private val messageStoreManager: MessageStoreManager,
    private val accountManager: AccountManager,
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
                syncClass = folder.syncClass,
                displayClass = folder.displayClass,
                isNotificationsEnabled = folder.isNotificationsEnabled,
                pushClass = folder.pushClass,
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
                syncClass = folder.syncClass,
                displayClass = folder.displayClass,
                isNotificationsEnabled = folder.isNotificationsEnabled,
                pushClass = folder.pushClass,
            )
        }
    }

    fun getPushFoldersFlow(account: Account): Flow<List<RemoteFolder>> {
        return account.getFolderPushModeFlow()
            .flatMapLatest { pushMode ->
                getPushFoldersFlow(account, pushMode)
            }
    }

    private fun getPushFoldersFlow(account: Account, folderMode: FolderMode): Flow<List<RemoteFolder>> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return callbackFlow {
            send(getPushFolders(account, folderMode))

            val listener = FolderSettingsChangedListener {
                trySendBlocking(getPushFolders(account, folderMode))
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private fun getPushFolders(account: Account, folderMode: FolderMode): List<RemoteFolder> {
        if (folderMode == FolderMode.NONE) return emptyList()

        return getRemoteFolderDetails(account)
            .asSequence()
            .filter { folderDetails ->
                val pushClass = folderDetails.effectivePushClass
                when (folderMode) {
                    FolderMode.NONE -> false
                    FolderMode.ALL -> true
                    FolderMode.FIRST_CLASS -> pushClass == FolderClass.FIRST_CLASS
                    FolderMode.FIRST_AND_SECOND_CLASS -> {
                        pushClass == FolderClass.FIRST_CLASS || pushClass == FolderClass.SECOND_CLASS
                    }
                    FolderMode.NOT_SECOND_CLASS -> pushClass != FolderClass.SECOND_CLASS
                }
            }
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

    fun setDisplayClass(account: Account, folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setDisplayClass(folderId, folderClass)
    }

    fun setSyncClass(account: Account, folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setSyncClass(folderId, folderClass)
    }

    fun setPushClass(account: Account, folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setPushClass(folderId, folderClass)
    }

    fun setNotificationsEnabled(account: Account, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setNotificationsEnabled(folderId, enable)
    }

    private fun Account.getFolderPushModeFlow(): Flow<FolderMode> {
        return accountManager.getAccountFlow(uuid).map { it.folderPushMode }
    }

    private val RemoteFolderDetails.effectivePushClass: FolderClass
        get() = if (pushClass == FolderClass.INHERITED) effectiveSyncClass else pushClass

    private val RemoteFolderDetails.effectiveSyncClass: FolderClass
        get() = if (syncClass == FolderClass.INHERITED) displayClass else syncClass
}

data class RemoteFolderDetails(
    val folder: RemoteFolder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val isNotificationsEnabled: Boolean,
    val pushClass: FolderClass,
)
