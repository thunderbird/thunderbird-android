package com.fsck.k9.storage.messages

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Header
import com.fsck.k9.mailstore.FolderMapper
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MessageStore

// TODO: Remove dependency on LocalStore
class K9MessageStore(private val localStore: LocalStore) : MessageStore {
    private val database: LockableDatabase = localStore.database
    private val threadMessageOperations = ThreadMessageOperations(localStore)
    private val moveMessageOperations = MoveMessageOperations(database, threadMessageOperations)
    private val flagMessageOperations = FlagMessageOperations(database)
    private val retrieveMessageOperations = RetrieveMessageOperations(database)
    private val retrieveFolderOperations = RetrieveFolderOperations(database)

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

    override fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T? {
        return retrieveFolderOperations.getFolder(folderId, mapper)
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
}
