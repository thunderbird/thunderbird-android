package com.fsck.k9.mailstore

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Header

/**
 * Functions for accessing and modifying locally stored messages.
 *
 * The goal is for this to gradually replace [LocalStore]. Once complete, apps will be able to provide their own
 * storage implementation.
 */
interface MessageStore {
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
     * Retrieve the server ID for a given message.
     */
    fun getMessageServerId(messageId: Long): String

    /**
     * Retrieve the server IDs for the given messages.
     *
     * @return A mapping of the message database ID to the message server ID.
     */
    fun getMessageServerIds(messageIds: Collection<Long>): Map<Long, String>

    /**
     * Retrieve the header fields of a message.
     */
    fun getHeaders(folderId: Long, messageServerId: String): List<Header>

    /**
     * Retrieve information about a folder.
     *
     * @param mapper A function to map the values read from the store to a domain-specific object.
     * @return The value returned by [mapper] or `null` if the folder wasn't found.
     */
    fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T?

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
}
