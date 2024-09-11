package com.fsck.k9.storage.messages

import app.k9mail.core.mail.folder.api.FolderDetails
import app.k9mail.legacy.account.Account.FolderMode
import app.k9mail.legacy.mailstore.CreateFolderInfo
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MoreMessages
import app.k9mail.legacy.mailstore.SaveMessageData
import app.k9mail.legacy.search.ConditionsTreeNode
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Header
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.message.extractors.BasicPartInfoExtractor
import java.util.Date

class K9MessageStore(
    database: LockableDatabase,
    storageManager: StorageManager,
    basicPartInfoExtractor: BasicPartInfoExtractor,
    accountUuid: String,
) : MessageStore {
    private val attachmentFileManager = AttachmentFileManager(storageManager, accountUuid)
    private val threadMessageOperations = ThreadMessageOperations()
    private val saveMessageOperations = SaveMessageOperations(
        database,
        attachmentFileManager,
        basicPartInfoExtractor,
        threadMessageOperations,
    )
    private val copyMessageOperations = CopyMessageOperations(database, attachmentFileManager, threadMessageOperations)
    private val moveMessageOperations = MoveMessageOperations(database, threadMessageOperations)
    private val flagMessageOperations = FlagMessageOperations(database)
    private val updateMessageOperations = UpdateMessageOperations(database)
    private val retrieveMessageOperations = RetrieveMessageOperations(database)
    private val retrieveMessageListOperations = RetrieveMessageListOperations(database)
    private val deleteMessageOperations = DeleteMessageOperations(database, attachmentFileManager)
    private val createFolderOperations = CreateFolderOperations(database)
    private val retrieveFolderOperations = RetrieveFolderOperations(database)
    private val checkFolderOperations = CheckFolderOperations(database)
    private val updateFolderOperations = UpdateFolderOperations(database)
    private val deleteFolderOperations = DeleteFolderOperations(database, attachmentFileManager)
    private val keyValueStoreOperations = KeyValueStoreOperations(database)
    private val databaseOperations = DatabaseOperations(database, storageManager, accountUuid)

    override fun saveRemoteMessage(folderId: Long, messageServerId: String, messageData: SaveMessageData) {
        saveMessageOperations.saveRemoteMessage(folderId, messageServerId, messageData)
    }

    override fun saveLocalMessage(folderId: Long, messageData: SaveMessageData, existingMessageId: Long?): Long {
        return saveMessageOperations.saveLocalMessage(folderId, messageData, existingMessageId)
    }

    override fun copyMessage(messageId: Long, destinationFolderId: Long): Long {
        return copyMessageOperations.copyMessage(messageId, destinationFolderId)
    }

    override fun moveMessage(messageId: Long, destinationFolderId: Long): Long {
        return moveMessageOperations.moveMessage(messageId, destinationFolderId)
    }

    override fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        flagMessageOperations.setFlag(messageIds, flag, set)
    }

    override fun setMessageFlag(folderId: Long, messageServerId: String, flag: Flag, set: Boolean) {
        flagMessageOperations.setMessageFlag(folderId, messageServerId, flag, set)
    }

    override fun setNewMessageState(folderId: Long, messageServerId: String, newMessage: Boolean) {
        updateMessageOperations.setNewMessageState(folderId, messageServerId, newMessage)
    }

    override fun clearNewMessageState() {
        updateMessageOperations.clearNewMessageState()
    }

    override fun getMessageServerId(messageId: Long): String? {
        return retrieveMessageOperations.getMessageServerId(messageId)
    }

    override fun getMessageServerIds(messageIds: Collection<Long>): Map<Long, String> {
        return retrieveMessageOperations.getMessageServerIds(messageIds)
    }

    override fun getMessageServerIds(folderId: Long): Set<String> {
        return retrieveMessageOperations.getMessageServerIds(folderId)
    }

    override fun isMessagePresent(folderId: Long, messageServerId: String): Boolean {
        return retrieveMessageOperations.isMessagePresent(folderId, messageServerId)
    }

    override fun getMessageFlags(folderId: Long, messageServerId: String): Set<Flag> {
        return retrieveMessageOperations.getMessageFlags(folderId, messageServerId)
    }

    override fun getAllMessagesAndEffectiveDates(folderId: Long): Map<String, Long?> {
        return retrieveMessageOperations.getAllMessagesAndEffectiveDates(folderId)
    }

    override fun <T> getMessages(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<out T?>,
    ): List<T> {
        return retrieveMessageListOperations.getMessages(selection, selectionArgs, sortOrder, messageMapper)
    }

    override fun <T> getThreadedMessages(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<out T?>,
    ): List<T> {
        return retrieveMessageListOperations.getThreadedMessages(selection, selectionArgs, sortOrder, messageMapper)
    }

    override fun <T> getThread(threadId: Long, sortOrder: String, messageMapper: MessageMapper<out T?>): List<T> {
        return retrieveMessageListOperations.getThread(threadId, sortOrder, messageMapper)
    }

    override fun getOldestMessageDate(folderId: Long): Date? {
        return retrieveMessageOperations.getOldestMessageDate(folderId)
    }

    override fun getHeaders(folderId: Long, messageServerId: String): List<Header> {
        return retrieveMessageOperations.getHeaders(folderId, messageServerId)
    }

    override fun getHeaders(folderId: Long, messageServerId: String, headerNames: Set<String>): List<Header> {
        return retrieveMessageOperations.getHeaders(folderId, messageServerId, headerNames)
    }

    override fun destroyMessages(folderId: Long, messageServerIds: Collection<String>) {
        deleteMessageOperations.destroyMessages(folderId, messageServerIds)
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
        mapper: FolderMapper<T>,
    ): List<T> {
        return retrieveFolderOperations.getDisplayFolders(displayMode, outboxFolderId, mapper)
    }

    override fun areAllIncludedInUnifiedInbox(folderIds: Collection<Long>): Boolean {
        return checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)
    }

    override fun getFolderId(folderServerId: String): Long? {
        return retrieveFolderOperations.getFolderId(folderServerId)
    }

    override fun getFolderServerId(folderId: Long): String? {
        return retrieveFolderOperations.getFolderServerId(folderId)
    }

    override fun getMessageCount(folderId: Long): Int {
        return retrieveFolderOperations.getMessageCount(folderId)
    }

    override fun getUnreadMessageCount(folderId: Long): Int {
        return retrieveFolderOperations.getUnreadMessageCount(folderId)
    }

    override fun getUnreadMessageCount(conditions: ConditionsTreeNode?): Int {
        return retrieveFolderOperations.getUnreadMessageCount(conditions)
    }

    override fun getStarredMessageCount(conditions: ConditionsTreeNode?): Int {
        return retrieveFolderOperations.getStarredMessageCount(conditions)
    }

    override fun getSize(): Long {
        return databaseOperations.getSize()
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

    override fun setPushClass(folderId: Long, folderClass: FolderClass) {
        updateFolderOperations.setPushClass(folderId, folderClass)
    }

    override fun setNotificationsEnabled(folderId: Long, enable: Boolean) {
        updateFolderOperations.setNotificationsEnabled(folderId, enable)
    }

    override fun hasMoreMessages(folderId: Long): MoreMessages {
        return retrieveFolderOperations.hasMoreMessages(folderId)
    }

    override fun setMoreMessages(folderId: Long, moreMessages: MoreMessages) {
        updateFolderOperations.setMoreMessages(folderId, moreMessages)
    }

    override fun setLastChecked(folderId: Long, timestamp: Long) {
        updateFolderOperations.setLastChecked(folderId, timestamp)
    }

    override fun setStatus(folderId: Long, status: String?) {
        updateFolderOperations.setStatus(folderId, status)
    }

    override fun setVisibleLimit(folderId: Long, visibleLimit: Int) {
        updateFolderOperations.setVisibleLimit(folderId, visibleLimit)
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

    override fun getFolderExtraString(folderId: Long, name: String): String? {
        return keyValueStoreOperations.getFolderExtraString(folderId, name)
    }

    override fun setFolderExtraString(folderId: Long, name: String, value: String?) {
        return keyValueStoreOperations.setFolderExtraString(folderId, name, value)
    }

    override fun getFolderExtraNumber(folderId: Long, name: String): Long? {
        return keyValueStoreOperations.getFolderExtraNumber(folderId, name)
    }

    override fun setFolderExtraNumber(folderId: Long, name: String, value: Long) {
        return keyValueStoreOperations.setFolderExtraNumber(folderId, name, value)
    }

    override fun compact() {
        return databaseOperations.compact()
    }
}
