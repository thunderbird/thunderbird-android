package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.filter.CountingOutputStream
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mail.internet.SizeAware
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.SaveMessageData
import com.fsck.k9.message.extractors.BasicPartInfoExtractor
import com.fsck.k9.message.extractors.PreviewResult.PreviewType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.Stack
import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.codec.Base64InputStream
import org.apache.james.mime4j.codec.QuotedPrintableInputStream
import org.apache.james.mime4j.util.MimeUtil

internal const val MAX_BODY_SIZE_FOR_DATABASE = 16 * 1024L

internal class SaveMessageOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
    private val partInfoExtractor: BasicPartInfoExtractor,
    private val threadMessageOperations: ThreadMessageOperations
) {
    fun saveRemoteMessage(folderId: Long, messageServerId: String, messageData: SaveMessageData) {
        lockableDatabase.execute(true) { database ->
            val message = messageData.message

            val threadInfo = threadMessageOperations.doMessageThreading(database, folderId, message.toThreadHeaders())

            val rootMessagePartId = saveMessageParts(database, message)
            val messageId = saveMessage(
                database,
                folderId,
                messageServerId,
                rootMessagePartId,
                messageData,
                emptyMessageId = threadInfo.messageId
            )

            if (threadInfo.threadId == null) {
                threadMessageOperations.createThreadEntry(database, messageId, threadInfo.rootId, threadInfo.parentId)
            }

            createOrReplaceFulltextEntry(database, messageId, messageData)
        }
    }

    private fun saveMessageParts(database: SQLiteDatabase, message: Message): Long {
        val rootPartContainer = PartContainer(parentId = null, part = message)
        val rootId = saveMessagePart(database, rootPartContainer, rootId = null, order = 0)

        val partsToSave = Stack<PartContainer>()
        addChildrenToStack(partsToSave, part = message, parentId = rootId)

        var order = 1
        while (partsToSave.isNotEmpty()) {
            val partContainer = partsToSave.pop()
            val messagePartId = saveMessagePart(database, partContainer, rootId, order)
            order++
            addChildrenToStack(partsToSave, partContainer.part, parentId = messagePartId)
        }

        return rootId
    }

    private fun saveMessagePart(
        database: SQLiteDatabase,
        partContainer: PartContainer,
        rootId: Long?,
        order: Int
    ): Long {
        val part = partContainer.part
        val values = ContentValues().apply {
            put("root", rootId)
            put("parent", partContainer.parentId ?: -1) // -1 for compatibility with previous code
            put("seq", order)
            put("server_extra", part.serverExtra)
        }

        return updateOrInsertMessagePart(database, values, part, existingMessagePartId = null)
    }

    private fun updateOrInsertMessagePart(
        database: SQLiteDatabase,
        values: ContentValues,
        part: Part,
        existingMessagePartId: Long?
    ): Long {
        val headerBytes = getHeaderBytes(part)
        values.put("mime_type", part.mimeType)
        values.put("header", headerBytes)
        values.put("type", MessagePartType.UNKNOWN)

        val file: File? = when (val body = part.body) {
            is Multipart -> multipartToContentValues(values, body)
            is Message -> messageMarkerToContentValues(values)
            null -> missingPartToContentValues(values, part)
            else -> leafPartToContentValues(values, part, body)
        }

        val messagePartId = if (existingMessagePartId != null) {
            database.update("message_parts", values, "id = ?", arrayOf(existingMessagePartId.toString()))
            existingMessagePartId
        } else {
            database.insertOrThrow("message_parts", null, values)
        }

        if (file != null) {
            attachmentFileManager.moveTemporaryFile(file, messagePartId)
        }

        return messagePartId
    }

    private fun multipartToContentValues(values: ContentValues, multipart: Multipart): File? {
        values.put("data_location", DataLocation.CHILD_PART_CONTAINS_DATA)
        values.put("preamble", multipart.preamble)
        values.put("epilogue", multipart.epilogue)
        values.put("boundary", multipart.boundary)

        return null
    }

    private fun messageMarkerToContentValues(cv: ContentValues): File? {
        cv.put("data_location", DataLocation.CHILD_PART_CONTAINS_DATA)

        return null
    }

    private fun missingPartToContentValues(values: ContentValues, part: Part): File? {
        val partInfo = partInfoExtractor.extractPartInfo(part)
        values.put("display_name", partInfo.displayName)
        values.put("data_location", DataLocation.MISSING)
        values.put("decoded_body_size", partInfo.size)

        if (MimeUtility.isMultipart(part.mimeType)) {
            values.put("boundary", BoundaryGenerator.getInstance().generateBoundary())
        }

        return null
    }

    private fun leafPartToContentValues(values: ContentValues, part: Part, body: Body): File? {
        val displayName = partInfoExtractor.extractDisplayName(part)
        values.put("display_name", displayName)

        val encoding = getTransferEncoding(part)
        values.put("encoding", encoding)
        values.put("content_id", part.contentId)

        check(body is SizeAware) { "Body needs to implement SizeAware" }
        val sizeAwareBody = body as SizeAware
        val fileSize = sizeAwareBody.size

        return if (fileSize > MAX_BODY_SIZE_FOR_DATABASE) {
            values.put("data_location", DataLocation.ON_DISK)
            val file = writeBodyToDiskIfNecessary(part)
            val size = decodeAndCountBytes(file, encoding, fileSize)
            values.put("decoded_body_size", size)

            file
        } else {
            values.put("data_location", DataLocation.IN_DATABASE)
            val bodyData = getBodyBytes(body)
            values.put("data", bodyData)
            val size = decodeAndCountBytes(bodyData.inputStream(), encoding, bodyData.size.toLong())
            values.put("decoded_body_size", size)

            null
        }
    }

    private fun writeBodyToDiskIfNecessary(part: Part): File? {
        val body = part.body
        return if (body is BinaryTempFileBody) {
            body.file
        } else {
            writeBodyToDisk(body)
        }
    }

    private fun writeBodyToDisk(body: Body): File? {
        val file = File.createTempFile("body", null, BinaryTempFileBody.getTempDirectory())
        FileOutputStream(file).use { outputStream ->
            body.writeTo(outputStream)
        }

        return file
    }

    private fun decodeAndCountBytes(file: File?, encoding: String, fallbackValue: Long): Long {
        return FileInputStream(file).use { inputStream ->
            decodeAndCountBytes(inputStream, encoding, fallbackValue)
        }
    }

    private fun decodeAndCountBytes(rawInputStream: InputStream, encoding: String, fallbackValue: Long): Long {
        return try {
            getDecodingInputStream(rawInputStream, encoding).use { decodingInputStream ->
                val countingOutputStream = CountingOutputStream()
                IOUtils.copy(decodingInputStream, countingOutputStream)
                countingOutputStream.count
            }
        } catch (e: IOException) {
            fallbackValue
        }
    }

    private fun getDecodingInputStream(rawInputStream: InputStream, encoding: String?): InputStream {
        return when (encoding) {
            MimeUtil.ENC_BASE64 -> {
                object : Base64InputStream(rawInputStream) {
                    override fun close() {
                        super.close()
                        rawInputStream.close()
                    }
                }
            }
            MimeUtil.ENC_QUOTED_PRINTABLE -> {
                object : QuotedPrintableInputStream(rawInputStream) {
                    override fun close() {
                        super.close()
                        rawInputStream.close()
                    }
                }
            }
            else -> {
                rawInputStream
            }
        }
    }

    private fun getHeaderBytes(part: Part): ByteArray {
        val output = ByteArrayOutputStream()
        part.writeHeaderTo(output)

        return output.toByteArray()
    }

    private fun getBodyBytes(body: Body): ByteArray {
        val output = ByteArrayOutputStream()
        body.writeTo(output)

        return output.toByteArray()
    }

    private fun getTransferEncoding(part: Part): String {
        val contentTransferEncoding = part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING).firstOrNull()
        return contentTransferEncoding?.toLowerCase(Locale.ROOT) ?: MimeUtil.ENC_7BIT
    }

    private fun addChildrenToStack(stack: Stack<PartContainer>, part: Part, parentId: Long) {
        when (val body = part.body) {
            is Multipart -> {
                for (i in body.count - 1 downTo 0) {
                    val childPart = body.getBodyPart(i)
                    stack.push(PartContainer(parentId, childPart))
                }
            }
            is Message -> {
                stack.push(PartContainer(parentId, body))
            }
        }
    }

    private fun saveMessage(
        database: SQLiteDatabase,
        folderId: Long,
        messageServerId: String,
        rootMessagePartId: Long,
        messageData: SaveMessageData,
        emptyMessageId: Long?
    ): Long {
        val message = messageData.message

        if (messageData.partialMessage) {
            message.setFlag(Flag.X_DOWNLOADED_PARTIAL, true)
        } else {
            message.setFlag(Flag.X_DOWNLOADED_FULL, true)
        }

        val values = ContentValues().apply {
            put("folder_id", folderId)
            put("uid", messageServerId)
            put("deleted", 0)
            put("empty", 0)
            put("message_part_id", rootMessagePartId)
            put("date", messageData.date)
            put("internal_date", messageData.internalDate)
            put("subject", messageData.subject)
            put("flags", message.flags.toDatabaseValue())
            put("read", message.isSet(Flag.SEEN).toDatabaseValue())
            put("flagged", message.isSet(Flag.FLAGGED).toDatabaseValue())
            put("answered", message.isSet(Flag.ANSWERED).toDatabaseValue())
            put("forwarded", message.isSet(Flag.FORWARDED).toDatabaseValue())
            put("sender_list", Address.pack(message.from))
            put("to_list", Address.pack(message.getRecipients(RecipientType.TO)))
            put("cc_list", Address.pack(message.getRecipients(RecipientType.CC)))
            put("bcc_list", Address.pack(message.getRecipients(RecipientType.BCC)))
            put("reply_to_list", Address.pack(message.replyTo))
            put("attachment_count", messageData.attachmentCount)
            put("message_id", message.messageId)
            put("mime_type", message.mimeType)
            put("encryption_type", messageData.encryptionType)

            val previewResult = messageData.previewResult
            put("preview_type", previewResult.previewType.toDatabaseValue())
            if (previewResult.isPreviewTextAvailable) {
                put("preview", previewResult.previewText)
            } else {
                putNull("preview")
            }
        }

        return if (emptyMessageId != null) {
            values.put("id", emptyMessageId)
            database.replace("messages", null, values)
            emptyMessageId
        } else {
            database.insert("messages", null, values)
        }
    }

    private fun createOrReplaceFulltextEntry(database: SQLiteDatabase, messageId: Long, messageData: SaveMessageData) {
        val fulltext = messageData.textForSearchIndex ?: return

        val values = ContentValues().apply {
            put("docid", messageId)
            put("fulltext", fulltext)
        }

        database.replace("messages_fulltext", null, values)
    }
}

private fun Set<Flag>.toDatabaseValue(): String {
    return this
        .filter { it !in SPECIAL_FLAGS }
        .joinToString(separator = ",")
}

private fun Boolean.toDatabaseValue() = if (this) 1 else 0

private fun PreviewType.toDatabaseValue(): String {
    return DatabasePreviewType.fromPreviewType(this).databaseValue
}

// Note: The contents of the 'message_parts' table depend on these values.
// TODO: currently unused, might be used for caching at a later point
internal object MessagePartType {
    const val UNKNOWN = 0
}

// Note: The contents of the 'message_parts' table depend on these values.
internal object DataLocation {
    const val MISSING = 0
    const val IN_DATABASE = 1
    const val ON_DISK = 2
    const val CHILD_PART_CONTAINS_DATA = 3
}

private data class PartContainer(val parentId: Long?, val part: Part)
