package com.fsck.k9.mailstore

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.FolderMode
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.di.DI
import app.k9mail.legacy.folder.DisplayFolder
import app.k9mail.legacy.folder.Folder
import app.k9mail.legacy.folder.FolderDetails
import app.k9mail.legacy.folder.FolderType
import app.k9mail.legacy.folder.RemoteFolder
import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
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
import com.fsck.k9.mail.FolderType as RemoteFolderType

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepository(
    private val messageStoreManager: MessageStoreManager,
    private val accountManager: AccountManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val sortForDisplay =
        compareByDescending<DisplayFolder> { it.folder.type == FolderType.INBOX }
            .thenByDescending { it.folder.type == FolderType.OUTBOX }
            .thenByDescending { it.folder.type != FolderType.REGULAR }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.folder.name }

    fun getDisplayFolders(account: Account, displayMode: FolderMode?): List<DisplayFolder> {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getDisplayFolders(
            displayMode = displayMode ?: account.folderDisplayMode,
            outboxFolderId = account.outboxFolderId,
        ) { folder ->
            DisplayFolder(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folderTypeOf(account, folder.id),
                    isLocalOnly = folder.isLocalOnly,
                ),
                isInTopGroup = folder.isInTopGroup,
                unreadMessageCount = folder.unreadMessageCount,
                starredMessageCount = folder.starredMessageCount,
            )
        }.sortedWith(sortForDisplay)
    }

    fun getDisplayFoldersFlow(account: Account, displayMode: FolderMode): Flow<List<DisplayFolder>> {
        val messagingController = DI.get<MessagingController>()
        val messageStore = messageStoreManager.getMessageStore(account)

        return callbackFlow {
            send(getDisplayFolders(account, displayMode))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(statusChangedAccount: Account, folderId: Long) {
                    if (statusChangedAccount.uuid == account.uuid) {
                        trySendBlocking(getDisplayFolders(account, displayMode))
                    }
                }
            }
            messagingController.addListener(folderStatusChangedListener)

            val folderSettingsChangedListener = FolderSettingsChangedListener {
                trySendBlocking(getDisplayFolders(account, displayMode))
            }
            messageStore.addFolderSettingsChangedListener(folderSettingsChangedListener)

            awaitClose {
                messagingController.removeListener(folderStatusChangedListener)
                messageStore.removeFolderSettingsChangedListener(folderSettingsChangedListener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getDisplayFoldersFlow(account: Account): Flow<List<DisplayFolder>> {
        return accountManager.getAccountFlow(account.uuid)
            .map { latestAccount ->
                AccountContainer(latestAccount, latestAccount.folderDisplayMode)
            }
            .distinctUntilChanged()
            .flatMapLatest { (account, folderDisplayMode) ->
                getDisplayFoldersFlow(account, folderDisplayMode)
            }
    }

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
                notifyClass = folder.notifyClass,
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
                notifyClass = folder.notifyClass,
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

    fun setNotificationClass(account: Account, folderId: Long, folderClass: FolderClass) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setNotificationClass(folderId, folderClass)
    }

    private fun folderTypeOf(account: Account, folderId: Long) = when (folderId) {
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

private data class AccountContainer(
    val account: Account,
    val folderDisplayMode: FolderMode,
)

data class RemoteFolderDetails(
    val folder: RemoteFolder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val notifyClass: FolderClass,
    val pushClass: FolderClass,
)
