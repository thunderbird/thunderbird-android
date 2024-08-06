package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MoreMessages
import app.k9mail.legacy.mailstore.SaveMessageData
import com.fsck.k9.mail.Flag

/**
 * [MessageStore] wrapper that triggers notifications on certain changes to the message store.
 */
class NotifierMessageStore(
    private val messageStore: MessageStore,
    private val localStore: LocalStore,
) : MessageStore by messageStore {

    override fun saveRemoteMessage(folderId: Long, messageServerId: String, messageData: SaveMessageData) {
        messageStore.saveRemoteMessage(folderId, messageServerId, messageData)
        notifyChange()
    }

    override fun saveLocalMessage(folderId: Long, messageData: SaveMessageData, existingMessageId: Long?): Long {
        return messageStore.saveLocalMessage(folderId, messageData, existingMessageId).also {
            notifyChange()
        }
    }

    override fun copyMessage(messageId: Long, destinationFolderId: Long): Long {
        return messageStore.copyMessage(messageId, destinationFolderId).also {
            notifyChange()
        }
    }

    override fun moveMessage(messageId: Long, destinationFolderId: Long): Long {
        return messageStore.moveMessage(messageId, destinationFolderId).also {
            notifyChange()
        }
    }

    override fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        messageStore.setFlag(messageIds, flag, set)
        notifyChange()
    }

    override fun setMessageFlag(folderId: Long, messageServerId: String, flag: Flag, set: Boolean) {
        messageStore.setMessageFlag(folderId, messageServerId, flag, set)
        notifyChange()
    }

    override fun setNewMessageState(folderId: Long, messageServerId: String, newMessage: Boolean) {
        messageStore.setNewMessageState(folderId, messageServerId, newMessage)
        notifyChange()
    }

    override fun clearNewMessageState() {
        messageStore.clearNewMessageState()
        notifyChange()
    }

    override fun destroyMessages(folderId: Long, messageServerIds: Collection<String>) {
        messageStore.destroyMessages(folderId, messageServerIds)
        notifyChange()
    }

    override fun setMoreMessages(folderId: Long, moreMessages: MoreMessages) {
        messageStore.setMoreMessages(folderId, moreMessages)
        notifyChange()
    }

    private fun notifyChange() {
        localStore.notifyChange()
    }
}
