package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import java.util.Date
import com.fsck.k9.mailstore.MoreMessages as StoreMoreMessages

class K9BackendFolder(
    localStore: LocalStore,
    private val messageStore: MessageStore,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    folderServerId: String
) : BackendFolder {
    private val database = localStore.database
    private val databaseId: String
    private val folderId: Long
    override val name: String
    override val visibleLimit: Int

    init {
        data class Init(val folderId: Long, val name: String, val visibleLimit: Int)

        val init = messageStore.getFolder(folderServerId) { folder ->
            Init(
                folderId = folder.id,
                name = folder.name,
                visibleLimit = folder.visibleLimit
            )
        } ?: error("Couldn't find folder $folderServerId")

        databaseId = init.folderId.toString()
        folderId = init.folderId
        name = init.name
        visibleLimit = init.visibleLimit
    }

    override fun getLastUid(): Long? {
        return messageStore.getLastUid(folderId)
    }

    override fun getMessageServerIds(): Set<String> {
        return messageStore.getMessageServerIds(folderId)
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        return messageStore.getAllMessagesAndEffectiveDates(folderId)
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun clearAllMessages() {
        val messageServerIds = messageStore.getMessageServerIds(folderId)
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun getMoreMessages(): MoreMessages {
        return messageStore.getFolder(folderId) { folder ->
            folder.moreMessages.toMoreMessages()
        } ?: MoreMessages.UNKNOWN
    }

    override fun setMoreMessages(moreMessages: MoreMessages) {
        messageStore.setMoreMessages(folderId, moreMessages.toStoreMoreMessages())
    }

    override fun setLastChecked(timestamp: Long) {
        messageStore.setLastUpdated(folderId, timestamp)
    }

    override fun setStatus(status: String?) {
        messageStore.setStatus(folderId, status)
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        return messageStore.isMessagePresent(folderId, messageServerId)
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        return messageStore.getMessageFlags(folderId, messageServerId)
    }

    override fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean) {
        messageStore.setMessageFlag(folderId, messageServerId, flag, value)
    }

    override fun saveCompleteMessage(message: Message) {
        saveMessage(message, partialMessage = false)
    }

    override fun savePartialMessage(message: Message) {
        saveMessage(message, partialMessage = true)
    }

    private fun saveMessage(message: Message, partialMessage: Boolean) {
        requireMessageServerId(message)

        val messageData = saveMessageDataCreator.createSaveMessageData(message, partialMessage)
        messageStore.saveRemoteMessage(folderId, message.uid, messageData)
    }

    override fun getOldestMessageDate(): Date? {
        return database.rawQuery("SELECT MIN(date) FROM messages WHERE folder_id = ?", databaseId) { cursor ->
            if (cursor.moveToFirst()) {
                Date(cursor.getLong(0))
            } else {
                null
            }
        }
    }

    override fun getFolderExtraString(name: String): String? {
        return database.execute(false) { db ->
            val cursor = db.query(
                "folder_extra_values",
                arrayOf("value_text"),
                "name = ? AND folder_id = ?",
                arrayOf(name, databaseId),
                null, null, null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    it.getStringOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setFolderExtraString(name: String, value: String?) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                if (value != null) {
                    put("value_text", value)
                } else {
                    putNull("value_text")
                }
                put("folder_id", databaseId)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun getFolderExtraNumber(name: String): Long? {
        return database.execute(false) { db ->
            val cursor = db.query(
                "folder_extra_values",
                arrayOf("value_integer"),
                "name = ? AND folder_id = ?",
                arrayOf(name, databaseId),
                null, null, null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    it.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setFolderExtraNumber(name: String, value: Long) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_integer", value)
                put("folder_id", databaseId)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun StoreMoreMessages.toMoreMessages(): MoreMessages = when (this) {
        StoreMoreMessages.UNKNOWN -> MoreMessages.UNKNOWN
        StoreMoreMessages.FALSE -> MoreMessages.FALSE
        StoreMoreMessages.TRUE -> MoreMessages.TRUE
    }

    private fun MoreMessages.toStoreMoreMessages(): StoreMoreMessages = when (this) {
        MoreMessages.UNKNOWN -> StoreMoreMessages.UNKNOWN
        MoreMessages.FALSE -> StoreMoreMessages.FALSE
        MoreMessages.TRUE -> StoreMoreMessages.TRUE
    }

    private fun requireMessageServerId(message: Message) {
        if (message.uid.isNullOrEmpty()) {
            error("Message requires a server ID to be set")
        }
    }
}
