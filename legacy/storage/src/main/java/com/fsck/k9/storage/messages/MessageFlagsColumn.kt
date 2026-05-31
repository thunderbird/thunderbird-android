package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import net.thunderbird.core.common.mail.Flag

internal val SPECIAL_FLAGS = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)

internal fun SQLiteDatabase.updateMessageFlagsById(
    messageIds: Collection<Long>,
    transform: (Set<Flag>) -> Set<Flag>,
) {
    performChunkedOperation(
        arguments = messageIds,
        argumentTransformation = Long::toString,
    ) { selectionSet, selectionArguments ->
        query(
            "messages",
            arrayOf("id", "flags"),
            "id $selectionSet",
            selectionArguments,
            null,
            null,
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val messageId = cursor.getLong(0)
                updateMessageFlags("id = ?", arrayOf(messageId.toString()), transform(cursor.getString(1).toFlagSet()))
            }
        }
    }
}

internal fun SQLiteDatabase.updateMessageFlagsByServerId(
    folderId: Long,
    messageServerId: String,
    transform: (Set<Flag>) -> Set<Flag>,
) {
    val oldFlags = query(
        "messages",
        arrayOf("flags"),
        "folder_id = ? AND uid = ?",
        arrayOf(folderId.toString(), messageServerId),
        null,
        null,
        null,
    ).use { cursor ->
        if (!cursor.moveToFirst()) error("Message not found $folderId:$messageServerId")

        cursor.getString(0).toFlagSet()
    }

    updateMessageFlags(
        selection = "folder_id = ? AND uid = ?",
        selectionArgs = arrayOf(folderId.toString(), messageServerId),
        flags = transform(oldFlags),
    )
}

internal fun Set<Flag>.toDatabaseValue(): String {
    return filter { it !in SPECIAL_FLAGS }
        .joinToString(separator = ",")
}

private fun String?.toFlagSet(): Set<Flag> {
    return if (isNullOrBlank()) {
        emptySet()
    } else {
        split(',')
            .filter { it.isNotEmpty() }
            .map { Flag.valueOf(it) }
            .toSet()
    }
}

private fun SQLiteDatabase.updateMessageFlags(
    selection: String,
    selectionArgs: Array<String>,
    flags: Set<Flag>,
) {
    val values = ContentValues().apply {
        put("flags", flags.toDatabaseValue())
    }

    update("messages", values, selection, selectionArgs)
}
