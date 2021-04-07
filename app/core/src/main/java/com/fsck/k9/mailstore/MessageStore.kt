package com.fsck.k9.mailstore

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
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
     * Find a folder with the given server ID and return its store ID.
     */
    fun getFolderId(folderServerId: String): Long?

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
     * Update the notification class of a folder.
     */
    fun setNotificationClass(folderId: Long, folderClass: FolderClass)

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
}
