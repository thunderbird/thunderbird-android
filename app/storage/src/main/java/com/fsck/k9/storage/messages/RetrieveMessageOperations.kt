package com.fsck.k9.storage.messages

import androidx.core.database.getLongOrNull
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.message.MessageHeaderParser
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MessageNotFoundException

internal class RetrieveMessageOperations(private val lockableDatabase: LockableDatabase) {

    fun getMessageServerId(messageId: Long): String {
        return lockableDatabase.execute(false) { database ->
            database.query(
                "messages",
                arrayOf("uid"),
                "id = ?",
                arrayOf(messageId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    throw MessagingException("Message [ID: $messageId] not found in database")
                }
            }
        }
    }

    fun getMessageServerIds(messageIds: Collection<Long>): Map<Long, String> {
        if (messageIds.isEmpty()) return emptyMap()

        return lockableDatabase.execute(false) { database ->
            val databaseIdToServerIdMapping = mutableMapOf<Long, String>()
            performChunkedOperation(
                arguments = messageIds,
                argumentTransformation = Long::toString
            ) { selectionSet, selectionArguments ->
                database.query(
                    "messages",
                    arrayOf("id", "uid"),
                    "id $selectionSet",
                    selectionArguments,
                    null,
                    null,
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val databaseId = cursor.getLong(0)
                        val serverId = cursor.getString(1)

                        databaseIdToServerIdMapping[databaseId] = serverId
                    }
                }
            }

            databaseIdToServerIdMapping
        }
    }

    fun getHeaders(folderId: Long, messageServerId: String): List<Header> {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT message_parts.header FROM messages" +
                    " LEFT JOIN message_parts ON (messages.message_part_id = message_parts.id)" +
                    " WHERE messages.folder_id = ? AND messages.uid = ?",
                arrayOf(folderId.toString(), messageServerId),
            ).use { cursor ->
                if (!cursor.moveToFirst()) throw MessageNotFoundException(folderId, messageServerId)

                val headerBytes = cursor.getBlob(0)

                val header = MimeHeader()
                MessageHeaderParser.parse(headerBytes.inputStream()) { name, value ->
                    header.addRawHeader(name, value)
                }

                header.headers
            }
        }
    }

    fun getLastUid(folderId: Long): Long? {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT MAX(uid) FROM messages WHERE folder_id = ?",
                arrayOf(folderId.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }
}
