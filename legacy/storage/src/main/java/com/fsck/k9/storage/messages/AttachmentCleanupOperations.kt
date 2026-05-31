package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.mailstore.AttachmentCleanupResult
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.LockableDatabase
import net.thunderbird.core.common.mail.Flag

internal class AttachmentCleanupOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
) {
    fun removeOldDownloadedAttachments(cutoffTime: Long, maxParts: Int): AttachmentCleanupResult {
        require(maxParts > 0) { "maxParts must be greater than zero" }

        var changedPartCount = 0
        var lastScannedPartId = 0L

        while (changedPartCount < maxParts) {
            val batchSize = minOf(CLEANUP_BATCH_SIZE, maxParts - changedPartCount)
            val attachmentParts = lockableDatabase.execute(true) { database ->
                val attachmentParts = database.getDownloadedAttachmentParts(cutoffTime, lastScannedPartId, batchSize)
                if (attachmentParts.parts.isNotEmpty()) {
                    attachmentParts.parts
                        .filter { it.dataLocation == DataLocation.ON_DISK }
                        .forEach { attachmentFileManager.deleteFile(it.messagePartId) }

                    val changedPartIds = attachmentParts.parts.map { it.messagePartId }
                    database.markPartsAsMissing(changedPartIds)

                    val changedMessageIds = attachmentParts.parts.map { it.messageId }.distinct()
                    database.markMessagesAsPartiallyDownloaded(changedMessageIds)
                }

                attachmentParts
            }

            changedPartCount += attachmentParts.parts.size
            lastScannedPartId = attachmentParts.lastScannedPartId ?: break
        }

        return AttachmentCleanupResult(
            changedPartCount = changedPartCount,
        )
    }

    private fun SQLiteDatabase.getDownloadedAttachmentParts(
        cutoffTime: Long,
        lastScannedPartId: Long,
        batchSize: Int,
    ): DownloadedAttachmentParts {
        var lastPartId: Long? = null

        return rawQuery(
            GET_DOWNLOADED_ATTACHMENT_PARTS_SQL,
            arrayOf(cutoffTime.toString(), lastScannedPartId.toString(), "%content-disposition%", batchSize.toString()),
        ).use { cursor ->
            val parts = buildList {
                while (cursor.moveToNext()) {
                    lastPartId = cursor.getLong(PART_ID_COLUMN_INDEX)
                    val header = cursor.getBlob(HEADER_COLUMN_INDEX)
                    if (header.isExplicitAttachment()) {
                        add(
                            DownloadedAttachmentPart(
                                messagePartId = cursor.getLong(PART_ID_COLUMN_INDEX),
                                dataLocation = cursor.getInt(DATA_LOCATION_COLUMN_INDEX),
                                messageId = cursor.getLong(MESSAGE_ID_COLUMN_INDEX),
                            ),
                        )
                    }
                }
            }
            DownloadedAttachmentParts(parts = parts, lastScannedPartId = lastPartId)
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
        updateMessageFlagsById(messageIds) { flags ->
            flags - Flag.X_DOWNLOADED_FULL + Flag.X_DOWNLOADED_PARTIAL
        }
    }

    companion object {
        private const val CLEANUP_BATCH_SIZE = 500
        private const val PART_ID_COLUMN_INDEX = 0
        private const val DATA_LOCATION_COLUMN_INDEX = 1
        private const val MESSAGE_ID_COLUMN_INDEX = 2
        private const val HEADER_COLUMN_INDEX = 3
        private const val GET_DOWNLOADED_ATTACHMENT_PARTS_SQL = """
SELECT message_parts.id, message_parts.data_location, messages.id, message_parts.header
FROM message_parts
JOIN messages ON messages.message_part_id = message_parts.root
JOIN folders ON folders.id = messages.folder_id
WHERE folders.local_only = 0
  AND messages.deleted = 0
  AND messages.empty = 0
  AND messages.encryption_type IS NULL
  AND COALESCE(NULLIF(messages.internal_date, 0), messages.date) < CAST(? AS INTEGER)
  AND message_parts.id > CAST(? AS INTEGER)
  AND message_parts.data_location IN (${DataLocation.IN_DATABASE}, ${DataLocation.ON_DISK})
  AND lower(CAST(message_parts.header AS TEXT)) LIKE ?
ORDER BY message_parts.id
LIMIT ?
        """
    }
}

private fun ByteArray?.isExplicitAttachment(): Boolean {
    return this?.toString(Charsets.UTF_8)
        ?.extractHeaderBody(MimeHeader.HEADER_CONTENT_DISPOSITION)
        ?.let(MimeUtility::unfold)
        ?.let { MimeUtility.getHeaderParameter(it, null).equals("attachment", ignoreCase = true) }
        ?: false
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

private data class DownloadedAttachmentParts(
    val parts: List<DownloadedAttachmentPart>,
    val lastScannedPartId: Long?,
)
