package com.fsck.k9.storage.messages

import com.fsck.k9.mail.Flag
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
}
