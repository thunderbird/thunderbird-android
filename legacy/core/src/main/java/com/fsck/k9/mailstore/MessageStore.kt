package com.fsck.k9.mailstore

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Header
import com.fsck.k9.search.ConditionsTreeNode
import java.util.Date

/**
 * Functions for accessing and modifying locally stored messages.
 *
 * The goal is for this to gradually replace [LocalStore]. Once complete, apps will be able to provide their own
 * storage implementation.
 */
interface MessageStore {
    /**
     * Save a remote message in this store.
     */
    fun saveRemoteMessage(folderId: Long, messageServerId: String, messageData: SaveMessageData)

    /**
     * Save a local message in this store.
     *
     * @param existingMessageId The message with this ID is replaced if not `null`.
     * @return The message ID of the saved message.
     */
    fun saveLocalMessage(folderId: Long, messageData: SaveMessageData, existingMessageId: Long? = null): Long

    /**
     * Copy a message to another folder.
     *
     * @return The message's database ID in the destination folder.
     */
    fun copyMessage(messageId: Long, destinationFolderId: Long): Long

    /**
     * Copy messages to another folder.
     *
     * @return A mapping of the original message database ID to the new message database ID.
     */
    fun copyMessages(messageIds: Collection<Long>, destinationFolderId: Long): Map<Long, Long> {
        return messageIds
            .map { messageId ->
                messageId to copyMessage(messageId, destinationFolderId)
            }
            .toMap()
    }

    /**
     * Move a message to another folder.
     *
     * @return The message's database ID in the destination folder. This will most likely be different from the
     *   messageId passed to this function.
     */
    fun moveMessage(messageId: Long, destinationFolderId: Long): Long

    /**
     * Move messages to another folder.
     *
     * @return A mapping of the original message database ID to the new message database ID.
     */
    fun moveMessages(messageIds: Collection<Long>, destinationFolderId: Long): Map<Long, Long> {
        return messageIds
            .map { messageId ->
                messageId to moveMessage(messageId, destinationFolderId)
            }
            .toMap()
    }

    /**
     * Set message flags.
     */
    fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean)

    /**
     * Set or remove a flag on a message.
     */
    fun setMessageFlag(folderId: Long, messageServerId: String, flag: Flag, set: Boolean)

    /**
     * Set whether a message should be considered as new.
     */
    fun setNewMessageState(folderId: Long, messageServerId: String, newMessage: Boolean)

    /**
     * Clear the new message state for all messages.
     */
    fun clearNewMessageState()

    /**
     * Retrieve the server ID for a given message.
     */
    fun getMessageServerId(messageId: Long): String?

    /**
     * Retrieve the server IDs for the given messages.
     *
     * @return A mapping of the message database ID to the message server ID.
     */
    fun getMessageServerIds(messageIds: Collection<Long>): Map<Long, String>

    /**
     * Retrieve server IDs for all remote messages in the given folder.
     */
    fun getMessageServerIds(folderId: Long): Set<String>

    /**
     * Check if a message is present in the store.
     */
    fun isMessagePresent(folderId: Long, messageServerId: String): Boolean

    /**
     * Get the flags associated with a message.
     */
    fun getMessageFlags(folderId: Long, messageServerId: String): Set<Flag>

    /**
     * Retrieve server IDs and dates for all remote messages in the given folder.
     */
    fun getAllMessagesAndEffectiveDates(folderId: Long): Map<String, Long?>

    /**
     * Retrieve list of messages.
     */
    fun <T> getMessages(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<out T?>,
    ): List<T>

    /**
     * Retrieve threaded list of messages.
     */
    fun <T> getThreadedMessages(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<out T?>,
    ): List<T>

    /**
     * Retrieve list of messages in a thread.
     */
    fun <T> getThread(threadId: Long, sortOrder: String, messageMapper: MessageMapper<out T?>): List<T>

    /**
     * Retrieve the date of the oldest message in the given folder.
     */
    fun getOldestMessageDate(folderId: Long): Date?

    /**
     * Retrieve the header fields of a message.
     */
    fun getHeaders(folderId: Long, messageServerId: String): List<Header>

    /**
     * Retrieve selected header fields of a message.
     */
    fun getHeaders(folderId: Long, messageServerId: String, headerNames: Set<String>): List<Header>

    /**
     * Return the size of this message store in bytes.
     */
    fun getSize(): Long

    /**
     * Remove messages from the store.
     */
    fun destroyMessages(folderId: Long, messageServerIds: Collection<String>)

    /**
     * Create folders.
     */
    fun createFolders(folders: List<CreateFolderInfo>)

    /**
     * Retrieve information about a folder.
     *
     * @param mapper A function to map the values read from the store to a domain-specific object.
     * @return The value returned by [mapper] or `null` if the folder wasn't found.
     */
    fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T?

    /**
     * Retrieve information about a folder.
     *
     * @param mapper A function to map the values read from the store to a domain-specific object.
     * @return The value returned by [mapper] or `null` if the folder wasn't found.
     */
    fun <T> getFolder(folderServerId: String, mapper: FolderMapper<T>): T?

    /**
     * Retrieve folders.
     *
     * @param mapper A function to map the values read from the store to a domain-specific object.
     * @return A list of values returned by [mapper].
     */
    fun <T> getFolders(excludeLocalOnly: Boolean, mapper: FolderMapper<T>): List<T>

    /**
     * Retrieve folders for the given display mode along with their unread count.
     *
     * For the Outbox the total number of messages will be returned.
     */
    fun <T> getDisplayFolders(displayMode: FolderMode, outboxFolderId: Long?, mapper: FolderMapper<T>): List<T>

    /**
     * Check if all given folders are included in the Unified Inbox.
     */
    fun areAllIncludedInUnifiedInbox(folderIds: Collection<Long>): Boolean

    /**
     * Find a folder with the given server ID and return its store ID.
     */
    fun getFolderId(folderServerId: String): Long?

    /**
     * Find a folder with the given store ID and return its server ID.
     */
    fun getFolderServerId(folderId: Long): String?

    /**
     * Retrieve the number of messages in a folder.
     */
    fun getMessageCount(folderId: Long): Int

    /**
     * Retrieve the number of unread messages in a folder.
     */
    fun getUnreadMessageCount(folderId: Long): Int

    /**
     * Retrieve the number of unread messages matching [conditions].
     */
    fun getUnreadMessageCount(conditions: ConditionsTreeNode?): Int

    /**
     * Retrieve the number of starred messages matching [conditions].
     */
    fun getStarredMessageCount(conditions: ConditionsTreeNode?): Int

    /**
     * Update a folder's name and type.
     */
    fun changeFolder(folderServerId: String, name: String, type: FolderType)

    /**
     * Update settings of a single folder.
     */
    fun updateFolderSettings(folderDetails: FolderDetails)

    /**
     * Update the "integrate" setting of a folder.
     */
    fun setIncludeInUnifiedInbox(folderId: Long, includeInUnifiedInbox: Boolean)

    /**
     * Update the display class of a folder.
     */
    fun setDisplayClass(folderId: Long, folderClass: FolderClass)

    /**
     * Update the sync class of a folder.
     */
    fun setSyncClass(folderId: Long, folderClass: FolderClass)

    /**
     * Update the push class of a folder.
     */
    fun setPushClass(folderId: Long, folderClass: FolderClass)

    /**
     * Update the notification class of a folder.
     */
    fun setNotificationClass(folderId: Long, folderClass: FolderClass)

    /**
     * Get the 'more messages' state of a folder.
     */
    fun hasMoreMessages(folderId: Long): MoreMessages?

    /**
     * Update the 'more messages' state of a folder.
     */
    fun setMoreMessages(folderId: Long, moreMessages: MoreMessages)

    /**
     * Update the time when the folder was last checked for new messages.
     */
    fun setLastChecked(folderId: Long, timestamp: Long)

    /**
     * Update folder status message.
     */
    fun setStatus(folderId: Long, status: String?)

    /**
     * Update a folder's "visible limit" value.
     */
    fun setVisibleLimit(folderId: Long, visibleLimit: Int)

    /**
     * Delete folders.
     */
    fun deleteFolders(folderServerIds: List<String>)

    /**
     * Retrieve a string property by name.
     *
     * For everything that doesn't fit into existing structures this message store offers a generic key/value store.
     */
    fun getExtraString(name: String): String?

    /**
     * Create or update a string property.
     */
    fun setExtraString(name: String, value: String)

    /**
     * Retrieve a number property by name.
     */
    fun getExtraNumber(name: String): Long?

    /**
     * Create or update a number property.
     */
    fun setExtraNumber(name: String, value: Long)

    /**
     * Retrieve a string property associated with the given folder.
     */
    fun getFolderExtraString(folderId: Long, name: String): String?

    /**
     * Create or update a string property associated with the given folder.
     */
    fun setFolderExtraString(folderId: Long, name: String, value: String?)

    /**
     * Retrieve a number property associated with the given folder.
     */
    fun getFolderExtraNumber(folderId: Long, name: String): Long?

    /**
     * Create or update a number property associated with the given folder.
     */
    fun setFolderExtraNumber(folderId: Long, name: String, value: Long)

    /**
     * Optimize the message store with the goal of using the minimal amount of disk space.
     */
    fun compact()
}
