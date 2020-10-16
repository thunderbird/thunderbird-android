package com.fsck.k9.storage.messages

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LockableDatabase

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
                Unit
            }

            databaseIdToServerIdMapping
        }
    }
}
