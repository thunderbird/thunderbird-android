package com.fsck.k9.storage.messages

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Header
import com.fsck.k9.mailstore.CreateFolderInfo
import com.fsck.k9.mailstore.FolderDetails
import com.fsck.k9.mailstore.FolderMapper
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MessageStore
import com.fsck.k9.mailstore.StorageManager

// TODO: Remove dependency on LocalStore
class K9MessageStore(
    private val localStore: LocalStore,
    storageManager: StorageManager,
    accountUuid: String
) : MessageStore {
    private val database: LockableDatabase = localStore.database
    private val threadMessageOperations = ThreadMessageOperations(localStore)
    private val moveMessageOperations = MoveMessageOperations(database, threadMessageOperations)
    private val flagMessageOperations = FlagMessageOperations(database)
    private val retrieveMessageOperations = RetrieveMessageOperations(database)
    private val createFolderOperations = CreateFolderOperations(database)
    private val retrieveFolderOperations = RetrieveFolderOperations(database)
    private val updateFolderOperations = UpdateFolderOperations(database)
    private val deleteFolderOperations = DeleteFolderOperations(storageManager, database, accountUuid)
    private val keyValueStoreOperations = KeyValueStoreOperations(database)

    override fun moveMessage(messageId: Long, destinationFolderId: Long): Long {
        return moveMessageOperations.moveMessage(messageId, destinationFolderId).also {
            localStore.notifyChange()
        }
    }

    override fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        flagMessageOperations.setFlag(messageIds, flag, set)
        localStore.notifyChange()
    }

    override fun getMessageServerId(messageId: Long): String {
        return retrieveMessageOperations.getMessageServerId(messageId)
    }

    override fun getMessageServerIds(messageIds: Collection<Long>): Map<Long, String> {
        return retrieveMessageOperations.getMessageServerIds(messageIds)
    }

    override fun getHeaders(folderId: Long, messageServerId: String): List<Header> {
        return retrieveMessageOperations.getHeaders(folderId, messageServerId)
    }

    override fun createFolders(folders: List<CreateFolderInfo>) {
        createFolderOperations.createFolders(folders)
    }

    override fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T? {
        return retrieveFolderOperations.getFolder(folderId, mapper)
    }

    override fun <T> getFolder(folderServerId: String, mapper: FolderMapper<T>): T? {
        return retrieveFolderOperations.getFolder(folderServerId, mapper)
    }

    override fun <T> getFolders(excludeLocalOnly: Boolean, mapper: FolderMapper<T>): List<T> {
        return retrieveFolderOperations.getFolders(excludeLocalOnly, mapper)
    }

    override fun <T> getDisplayFolders(
        displayMode: FolderMode,
        outboxFolderId: Long?,
        mapper: FolderMapper<T>
    ): List<T> {
        return retrieveFolderOperations.getDisplayFolders(displayMode, outboxFolderId, mapper)
    }

    override fun getFolderId(folderServerId: String): Long? {
        return retrieveFolderOperations.getFolderId(folderServerId)
    }

    override fun changeFolder(folderServerId: String, name: String, type: FolderType) {
        updateFolderOperations.changeFolder(folderServerId, name, type)
    }

    override fun updateFolderSettings(folderDetails: FolderDetails) {
        updateFolderOperations.updateFolderSettings(folderDetails)
    }

    override fun setIncludeInUnifiedInbox(folderId: Long, includeInUnifiedInbox: Boolean) {
        updateFolderOperations.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
    }

    override fun setDisplayClass(folderId: Long, folderClass: FolderClass) {
        updateFolderOperations.setDisplayClass(folderId, folderClass)
    }

    override fun setSyncClass(folderId: Long, folderClass: FolderClass) {
        updateFolderOperations.setSyncClass(folderId, folderClass)
    }

    override fun setNotificationClass(folderId: Long, folderClass: FolderClass) {
        updateFolderOperations.setNotificationClass(folderId, folderClass)
    }

    override fun deleteFolders(folderServerIds: List<String>) {
        deleteFolderOperations.deleteFolders(folderServerIds)
    }

    override fun getExtraString(name: String): String? {
        return keyValueStoreOperations.getExtraString(name)
    }

    override fun setExtraString(name: String, value: String) {
        keyValueStoreOperations.setExtraString(name, value)
    }

    override fun getExtraNumber(name: String): Long? {
        return keyValueStoreOperations.getExtraNumber(name)
    }

    override fun setExtraNumber(name: String, value: Long) {
        keyValueStoreOperations.setExtraNumber(name, value)
    }
}
