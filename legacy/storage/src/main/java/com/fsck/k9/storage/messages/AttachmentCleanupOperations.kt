package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.LockableDatabase
import net.thunderbird.core.common.mail.Flag

internal class AttachmentCleanupOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
) {
    fun removeOldDownloadedAttachments(cutoffTime: Long): Int {
        return lockableDatabase.execute(true) { database ->
            val attachmentParts = database.getDownloadedAttachmentParts(cutoffTime)
            if (attachmentParts.isEmpty()) return@execute 0

            attachmentParts
                .filter { it.dataLocation == DataLocation.ON_DISK }
                .forEach { attachmentFileManager.deleteFile(it.messagePartId) }

            val changedPartIds = attachmentParts.map { it.messagePartId }
            database.markPartsAsMissing(changedPartIds)

            val changedMessageIds = attachmentParts.map { it.messageId }.distinct()
            database.markMessagesAsPartiallyDownloaded(changedMessageIds)

            changedPartIds.size
        }
    }

    private fun SQLiteDatabase.getDownloadedAttachmentParts(cutoffTime: Long): List<DownloadedAttachmentPart> {
        return rawQuery(
            """
SELECT message_parts.id, message_parts.data_location, messages.id, message_parts.header
FROM message_parts
JOIN messages ON messages.message_part_id = message_parts.root
JOIN folders ON folders.id = messages.folder_id
WHERE folders.local_only = 0
  AND messages.deleted = 0
  AND messages.empty = 0
  AND messages.encryption_type IS NULL
  AND COALESCE(NULLIF(messages.internal_date, 0), messages.date) < CAST(? AS INTEGER)
  AND message_parts.data_location IN (${DataLocation.IN_DATABASE}, ${DataLocation.ON_DISK})
  AND lower(CAST(message_parts.header AS TEXT)) LIKE ?
            """.trimIndent(),
            arrayOf(cutoffTime.toString(), "%content-disposition%"),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val header = cursor.getBlob(3)
                    if (header.isExplicitAttachment()) {
                        add(
                            DownloadedAttachmentPart(
                                messagePartId = cursor.getLong(0),
                                dataLocation = cursor.getInt(1),
                                messageId = cursor.getLong(2),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun SQLiteDatabase.markPartsAsMissing(messagePartIds: List<Long>) {
        performChunkedOperation(
            arguments = messagePartIds,
            argumentTransformation = Long::toString,
        ) { selectionSet, selectionArguments ->
            val values = ContentValues().apply {
                put("data_location", DataLocation.MISSING)
                putNull("data")
            }

            update("message_parts", values, "id $selectionSet", selectionArguments)
        }
    }

    private fun SQLiteDatabase.markMessagesAsPartiallyDownloaded(messageIds: List<Long>) {
        performChunkedOperation(
            arguments = messageIds,
            argumentTransformation = Long::toString,
        ) { selectionSet, selectionArguments ->
            rawQuery("SELECT id, flags FROM messages WHERE id $selectionSet", selectionArguments).use { cursor ->
                while (cursor.moveToNext()) {
                    val messageId = cursor.getLong(0)
                    val flags = if (cursor.isNull(1) || cursor.getString(1).isBlank()) {
                        mutableSetOf<Flag>()
                    } else {
                        cursor.getString(1)
                            .split(',')
                            .filter { it.isNotEmpty() }
                            .map { Flag.valueOf(it) }
                            .toMutableSet()
                    }

                    flags.remove(Flag.X_DOWNLOADED_FULL)
                    flags.add(Flag.X_DOWNLOADED_PARTIAL)

                    val values = ContentValues().apply {
                        put("flags", flags.joinToString(separator = ","))
                    }

                    update("messages", values, "id = ?", arrayOf(messageId.toString()))
                }
            }
        }
    }
}

private fun ByteArray?.isExplicitAttachment(): Boolean {
    if (this == null) return false

    val contentDisposition = toString(Charsets.UTF_8)
        .extractHeaderBody(MimeHeader.HEADER_CONTENT_DISPOSITION)
        ?: return false
    val dispositionType = MimeUtility.getHeaderParameter(MimeUtility.unfold(contentDisposition), null)

    return dispositionType.equals("attachment", ignoreCase = true)
}

private fun String.extractHeaderBody(headerName: String): String? {
    val lines = replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()

    val unfoldedLines = mutableListOf<String>()
    for (line in lines) {
        if (line.startsWith(" ") || line.startsWith("\t")) {
            if (unfoldedLines.isNotEmpty()) {
                unfoldedLines[unfoldedLines.lastIndex] += line
            }
        } else {
            unfoldedLines.add(line)
        }
    }

    for (line in unfoldedLines) {
        val separatorIndex = line.indexOf(':')
        if (separatorIndex <= 0) continue

        val name = line.substring(0, separatorIndex).trim()
        if (name.equals(headerName, ignoreCase = true)) {
            return line.substring(separatorIndex + 1).trim()
        }
    }

    return null
}

private data class DownloadedAttachmentPart(
    val messagePartId: Long,
    val dataLocation: Int,
    val messageId: Long,
)
