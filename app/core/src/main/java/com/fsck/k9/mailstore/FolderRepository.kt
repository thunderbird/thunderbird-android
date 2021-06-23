package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.preferences.AccountManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.fsck.k9.mail.FolderType as RemoteFolderType

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepository(
    private val messageStoreManager: MessageStoreManager,
    private val accountManager: AccountManager,
    private val account: Account,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val sortForDisplay =
        compareByDescending<DisplayFolder> { it.folder.type == FolderType.INBOX }
            .thenByDescending { it.folder.type == FolderType.OUTBOX }
            .thenByDescending { it.folder.type != FolderType.REGULAR }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.folder.name }

    fun getDisplayFolders(displayMode: FolderMode?): List<DisplayFolder> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getDisplayFolders(
            displayMode = displayMode ?: account.folderDisplayMode,
            outboxFolderId = account.outboxFolderId
        ) { folder ->
            DisplayFolder(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folderTypeOf(folder.id),
                    isLocalOnly = folder.isLocalOnly
                ),
                isInTopGroup = folder.isInTopGroup,
                unreadCount = folder.messageCount
            )
        }.sortedWith(sortForDisplay)
    }

    fun getFolder(folderId: Long): Folder? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            Folder(
                id = folder.id,
                name = folder.name,
                type = folderTypeOf(folder.id),
                isLocalOnly = folder.isLocalOnly
            )
        }
    }

    fun getFolderDetails(folderId: Long): FolderDetails? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            FolderDetails(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folderTypeOf(folder.id),
                    isLocalOnly = folder.isLocalOnly
                ),
                isInTopGroup = folder.isInTopGroup,
                isIntegrate = folder.isIntegrate,
                syncClass = folder.syncClass,
                displayClass = folder.displayClass,
                notifyClass = folder.notifyClass,
                pushClass = folder.pushClass
            )
        }
    }

    fun getRemoteFolders(): List<RemoteFolder> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
            RemoteFolder(
                id = folder.id,
                serverId = folder.serverIdOrThrow(),
                name = folder.name,
                type = folder.type.toFolderType()
            )
        }
    }

    fun getRemoteFolderDetails(): List<RemoteFolderDetails> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
            RemoteFolderDetails(
                folder = RemoteFolder(
                    id = folder.id,
                    serverId = folder.serverIdOrThrow(),
                    name = folder.name,
                    type = folder.type.toFolderType()
                ),
                isInTopGroup = folder.isInTopGroup,
                isIntegrate = folder.isIntegrate,
                syncClass = folder.syncClass,
                displayClass = folder.displayClass,
                notifyClass = folder.notifyClass,
                pushClass = folder.pushClass
            )
        }
    }

    fun getPushFoldersFlow(): Flow<List<RemoteFolder>> {
        return account.getFolderPushModeFlow()
            .flatMapLatest { pushMode ->
                getPushFoldersFlow(pushMode)
            }
    }

    private fun getPushFoldersFlow(folderMode: FolderMode): Flow<List<RemoteFolder>> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return callbackFlow {
            send(getPushFolders(folderMode))

            val listener = FolderSettingsChangedListener {
                launch {
                    send(getPushFolders(folderMode))
                }
            }
            messageStore.addFolderSettingsChangedListener(listener)

            awaitClose {
                messageStore.removeFolderSettingsChangedListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private fun getPushFolders(folderMode: FolderMode): List<RemoteFolder> {
        if (folderMode == FolderMode.NONE) return emptyList()

        return getRemoteFolderDetails()
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

    fun getFolderServerId(folderId: Long): String? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            folder.serverId
        }
    }

    fun getFolderId(folderServerId: String): Long? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolderId(folderServerId)
    }

    fun isFolderPresent(folderId: Long): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { true } ?: false
    }

    fun updateFolderDetails(folderDetails: FolderDetails) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.updateFolderSettings(folderDetails)
    }

    fun setIncludeInUnifiedInbox(folderId: Long, includeInUnifiedInbox: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    fun setDisplayClass(folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setDisplayClass(folderId, folderClass)
    }

    fun setSyncClass(folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setSyncClass(folderId, folderClass)
    }

    fun setNotificationClass(folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setNotificationClass(folderId, folderClass)
    }

    private fun folderTypeOf(folderId: Long) = when (folderId) {
        account.inboxFolderId -> FolderType.INBOX
        account.outboxFolderId -> FolderType.OUTBOX
        account.sentFolderId -> FolderType.SENT
        account.trashFolderId -> FolderType.TRASH
        account.draftsFolderId -> FolderType.DRAFTS
        account.archiveFolderId -> FolderType.ARCHIVE
        account.spamFolderId -> FolderType.SPAM
        else -> FolderType.REGULAR
    }

    private fun RemoteFolderType.toFolderType(): FolderType = when (this) {
        RemoteFolderType.REGULAR -> FolderType.REGULAR
        RemoteFolderType.INBOX -> FolderType.INBOX
        RemoteFolderType.OUTBOX -> FolderType.REGULAR // We currently don't support remote Outbox folders
        RemoteFolderType.DRAFTS -> FolderType.DRAFTS
        RemoteFolderType.SENT -> FolderType.SENT
        RemoteFolderType.TRASH -> FolderType.TRASH
        RemoteFolderType.SPAM -> FolderType.SPAM
        RemoteFolderType.ARCHIVE -> FolderType.ARCHIVE
    }

    private fun Account.getFolderPushModeFlow(): Flow<FolderMode> {
        return accountManager.getAccountFlow(uuid).map { it.folderPushMode }
    }

    private val RemoteFolderDetails.effectivePushClass: FolderClass
        get() = if (pushClass == FolderClass.INHERITED) effectiveSyncClass else pushClass

    private val RemoteFolderDetails.effectiveSyncClass: FolderClass
        get() = if (syncClass == FolderClass.INHERITED) displayClass else syncClass
}

data class Folder(val id: Long, val name: String, val type: FolderType, val isLocalOnly: Boolean)

data class RemoteFolder(val id: Long, val serverId: String, val name: String, val type: FolderType)

data class FolderDetails(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val notifyClass: FolderClass,
    val pushClass: FolderClass
)

data class RemoteFolderDetails(
    val folder: RemoteFolder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val notifyClass: FolderClass,
    val pushClass: FolderClass
)

data class DisplayFolder(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadCount: Int
)

enum class FolderType {
    REGULAR,
    INBOX,
    OUTBOX,
    SENT,
    TRASH,
    DRAFTS,
    ARCHIVE,
    SPAM
}
