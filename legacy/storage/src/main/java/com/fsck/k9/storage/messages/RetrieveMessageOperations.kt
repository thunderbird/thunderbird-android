package com.fsck.k9.storage.messages

import androidx.core.database.getLongOrNull
import com.fsck.k9.K9
import com.fsck.k9.helper.mapToSet
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.message.MessageHeaderParser
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MessageNotFoundException
import java.util.Date
import net.thunderbird.core.common.mail.Flag

internal class RetrieveMessageOperations(private val lockableDatabase: LockableDatabase) {

    fun getMessageServerId(messageId: Long): String? {
        return lockableDatabase.execute(false) { database ->
            database.query(
                "messages",
                arrayOf("uid"),
                "id = ?",
                arrayOf(messageId.toString()),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
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
                argumentTransformation = Long::toString,
            ) { selectionSet, selectionArguments ->
                database.query(
                    "messages",
                    arrayOf("id", "uid"),
                    "id $selectionSet",
                    selectionArguments,
                    null,
                    null,
                    null,
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

    fun getMessageServerIds(folderId: Long): Set<String> {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT uid FROM messages" +
                    " WHERE empty = 0 AND deleted = 0 AND folder_id = ? AND uid NOT LIKE '${K9.LOCAL_UID_PREFIX}%'",
                arrayOf(folderId.toString()),
            ).use { cursor ->
                val result = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val uid = cursor.getString(0)
                    result.add(uid)
                }
                result
            }
        }
    }

    fun isMessagePresent(folderId: Long, messageServerId: String): Boolean {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "messages",
                arrayOf("id"),
                "folder_id = ? AND uid = ?",
                arrayOf(folderId.toString(), messageServerId),
                null,
                null,
                null,
            ).use { cursor ->
                cursor.moveToFirst()
            }
        }
    }

    fun getMessageFlags(folderId: Long, messageServerId: String): Set<Flag> {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "messages",
                arrayOf("deleted", "read", "flagged", "answered", "forwarded", "flags"),
                "folder_id = ? AND uid = ?",
                arrayOf(folderId.toString(), messageServerId),
                null,
                null,
                null,
            ).use { cursor ->
                if (!cursor.moveToFirst()) error("Couldn't read flags for $folderId:$messageServerId")

                val deleted = cursor.getInt(0) == 1
                val read = cursor.getInt(1) == 1
                val flagged = cursor.getInt(2) == 1
                val answered = cursor.getInt(3) == 1
                val forwarded = cursor.getInt(4) == 1
                val flagsColumnValue = cursor.getString(5)

                val otherFlags = if (flagsColumnValue.isNullOrBlank()) {
                    emptySet()
                } else {
                    flagsColumnValue.split(',').map { Flag.valueOf(it) }
                }

                otherFlags
                    .toMutableSet()
                    .apply {
                        if (deleted) add(Flag.DELETED)
                        if (read) add(Flag.SEEN)
                        if (flagged) add(Flag.FLAGGED)
                        if (answered) add(Flag.ANSWERED)
                        if (forwarded) add(Flag.FORWARDED)
                    }
            }
        }
    }

    fun getAllMessagesAndEffectiveDates(folderId: Long): Map<String, Long?> {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT uid, date FROM messages" +
                    " WHERE empty = 0 AND deleted = 0 AND folder_id = ? AND uid NOT LIKE '${K9.LOCAL_UID_PREFIX}%'",
                arrayOf(folderId.toString()),
            ).use { cursor ->
                val result = mutableMapOf<String, Long?>()
                while (cursor.moveToNext()) {
                    val uid = cursor.getString(0)
                    val date = cursor.getLongOrNull(1)
                    result[uid] = date
                }
                result
            }
        }
    }

    fun getOldestMessageDate(folderId: Long): Date? {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT MIN(date) FROM messages WHERE folder_id = ?",
                arrayOf(folderId.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val timestamp = cursor.getLong(0)
                    if (timestamp != 0L) Date(timestamp) else null
                } else {
                    null
                }
            }
        }
    }

    fun getHeaders(folderId: Long, messageServerId: String, headerNames: Set<String>? = null): List<Header> {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                "SELECT message_parts.header FROM messages" +
                    " LEFT JOIN message_parts ON (messages.message_part_id = message_parts.id)" +
                    " WHERE messages.folder_id = ? AND messages.uid = ?",
                arrayOf(folderId.toString(), messageServerId),
            ).use { cursor ->
                if (!cursor.moveToFirst()) throw MessageNotFoundException(folderId, messageServerId)

                val headerBytes = cursor.getBlob(0)
                val lowercaseHeaderNames = headerNames?.mapToSet(headerNames.size) { it.lowercase() }

                val header = MimeHeader()
                MessageHeaderParser.parse(headerBytes.inputStream()) { name, value ->
                    if (lowercaseHeaderNames == null || name.lowercase() in lowercaseHeaderNames) {
                        header.addRawHeader(name, value)
                    }
                }

                header.headers
            }
        }
    }
}
