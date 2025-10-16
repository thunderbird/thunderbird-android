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
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.RemoteFolder

@Suppress("TooManyFunctions")
class FolderRepository(
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getFolder(account: LegacyAccountDto, folderId: Long): Folder? {
        val messageStore = messageStoreManager.getMessageStore(account)
        val outboxFolderId = outboxFolderManager.getOutboxFolderId(account.id)
        return messageStore.getFolder(folderId) { folder ->
            Folder(
                id = folder.id,
                name = folder.name,
                type = folder.getFolderType(account, outboxFolderId),
                isLocalOnly = folder.isLocalOnly,
            )
        }
    }

    suspend fun getFolderDetails(account: LegacyAccountDto, folderId: Long): FolderDetails? {
        val messageStore = messageStoreManager.getMessageStore(account)
        val outboxFolderId = outboxFolderManager.getOutboxFolderId(account.id)
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
    fun getRemoteFolders(accountUuid: String): List<RemoteFolder> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        return messageStore.getFolders(excludeLocalOnly = true) { folder ->
            RemoteFolder(
                id = folder.id,
                serverId = folder.serverIdOrThrow(),
                name = folder.name,
                type = folder.type.toFolderType(),
            )
        }
    }

    @Throws(MessagingException::class)
    fun getRemoteFolders(account: LegacyAccountDto): List<RemoteFolder> =
        getRemoteFolders(account.uuid)

    fun getRemoteFolderDetails(account: LegacyAccountDto): List<RemoteFolderDetails> {
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

    fun getPushFoldersFlow(account: LegacyAccountDto): Flow<List<RemoteFolder>> {
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

    private fun getPushFolders(account: LegacyAccountDto): List<RemoteFolder> {
        return getRemoteFolderDetails(account)
            .asSequence()
            .filter { folderDetails -> folderDetails.isPushEnabled }
            .map { folderDetails -> folderDetails.folder }
            .toList()
    }

    fun getFolderServerId(account: LegacyAccountDto, folderId: Long): String? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { folder ->
            folder.serverId
        }
    }

    fun getFolderId(account: LegacyAccountDto, folderServerId: String): Long? {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolderId(folderServerId)
    }

    fun isFolderPresent(account: LegacyAccountDto, folderId: Long): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.getFolder(folderId) { true } ?: false
    }

    fun updateFolderDetails(account: LegacyAccountDto, folderDetails: FolderDetails) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.updateFolderSettings(folderDetails)
    }

    fun setIncludeInUnifiedInbox(account: LegacyAccountDto, folderId: Long, includeInUnifiedInbox: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    fun setVisible(account: LegacyAccountDto, folderId: Long, visible: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setVisible(folderId, visible)
    }

    fun setSyncEnabled(account: LegacyAccountDto, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setSyncEnabled(folderId, enable)
    }

    fun setNotificationsEnabled(account: LegacyAccountDto, folderId: Long, enable: Boolean) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setNotificationsEnabled(folderId, enable)
    }

    fun setPushDisabled(account: LegacyAccountDto) {
        val messageStore = messageStoreManager.getMessageStore(account)
        messageStore.setPushDisabled()
    }

    fun hasPushEnabledFolder(account: LegacyAccountDto): Boolean {
        val messageStore = messageStoreManager.getMessageStore(account)
        return messageStore.hasPushEnabledFolder()
    }

    fun hasPushEnabledFolderFlow(account: LegacyAccountDto): Flow<Boolean> {
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

    private fun FolderDetailsAccessor.getFolderType(account: LegacyAccountDto, outboxFolderId: Long): FolderType =
        if (id == outboxFolderId) {
            FolderType.OUTBOX
        } else {
            folderTypeOf(account, id)
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
