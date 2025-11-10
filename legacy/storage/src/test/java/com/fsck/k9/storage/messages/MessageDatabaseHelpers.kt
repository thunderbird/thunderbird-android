package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getIntOrNull
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.getStringOrNull
import app.k9mail.core.android.common.database.map
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.storage.K9SchemaDefinitionFactory
import java.io.File
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

fun createLockableDatabaseMock(sqliteDatabase: SQLiteDatabase): LockableDatabase {
    return mock {
        on { execute(ArgumentMatchers.anyBoolean(), any<LockableDatabase.DbCallback<Any>>()) } doAnswer { stubbing ->
            val callback: LockableDatabase.DbCallback<Any> = stubbing.getArgument(1)
            callback.doDbWork(sqliteDatabase)
        }
    }
}

fun createDatabase(): SQLiteDatabase {
    val migrationsHelper = mock<MigrationsHelper>()

    val sqliteDatabase = SQLiteDatabase.create(null)
    val schemaDefinitionFactory = K9SchemaDefinitionFactory()
    val schemaDefinition = schemaDefinitionFactory.createSchemaDefinition(migrationsHelper)

    schemaDefinition.doDbUpgrade(sqliteDatabase)

    return sqliteDatabase
}

fun SQLiteDatabase.createMessage(
    folderId: Long,
    deleted: Boolean = false,
    uid: String? = null,
    subject: String = "",
    date: Long = 0L,
    flags: String = "",
    senderList: String = "",
    toList: String = "",
    ccList: String = "",
    bccList: String = "",
    replyToList: String = "",
    attachmentCount: Int = 0,
    internalDate: Long = 0L,
    messageIdHeader: String? = null,
    previewType: DatabasePreviewType = DatabasePreviewType.NONE,
    preview: String = "",
    mimeType: String = "text/plain",
    normalizedSubjectHash: Long = 0L,
    empty: Boolean = false,
    read: Boolean = false,
    flagged: Boolean = false,
    answered: Boolean = false,
    forwarded: Boolean = false,
    messagePartId: Long = 0L,
    encryptionType: String? = null,
    newMessage: Boolean = false,
): Long {
    val values = ContentValues().apply {
        put("deleted", if (deleted) 1 else 0)
        put("folder_id", folderId)
        put("uid", uid)
        put("subject", subject)
        put("date", date)
        put("flags", flags)
        put("sender_list", senderList)
        put("to_list", toList)
        put("cc_list", ccList)
        put("bcc_list", bccList)
        put("reply_to_list", replyToList)
        put("attachment_count", attachmentCount)
        put("internal_date", internalDate)
        put("message_id", messageIdHeader)
        put("preview_type", previewType.databaseValue)
        put("preview", preview)
        put("mime_type", mimeType)
        put("normalized_subject_hash", normalizedSubjectHash)
        put("empty", if (empty) 1 else 0)
        put("read", if (read) 1 else 0)
        put("flagged", if (flagged) 1 else 0)
        put("answered", if (answered) 1 else 0)
        put("forwarded", if (forwarded) 1 else 0)
        put("message_part_id", messagePartId)
        put("encryption_type", encryptionType)
        put("new_message", if (newMessage) 1 else 0)
    }

    return insert("messages", null, values)
}

fun SQLiteDatabase.readMessages(): List<MessageEntry> {
    val cursor = rawQuery("SELECT * FROM messages", null)
    return cursor.use {
        cursor.map {
            MessageEntry(
                id = cursor.getLongOrNull("id"),
                deleted = cursor.getIntOrNull("deleted"),
                folderId = cursor.getLongOrNull("folder_id"),
                uid = cursor.getStringOrNull("uid"),
                subject = cursor.getStringOrNull("subject"),
                date = cursor.getLongOrNull("date"),
                flags = cursor.getStringOrNull("flags"),
                senderList = cursor.getStringOrNull("sender_list"),
                toList = cursor.getStringOrNull("to_list"),
                ccList = cursor.getStringOrNull("cc_list"),
                bccList = cursor.getStringOrNull("bcc_list"),
                replyToList = cursor.getStringOrNull("reply_to_list"),
                attachmentCount = cursor.getIntOrNull("attachment_count"),
                internalDate = cursor.getLongOrNull("internal_date"),
                messageId = cursor.getStringOrNull("message_id"),
                previewType = cursor.getStringOrNull("preview_type"),
                preview = cursor.getStringOrNull("preview"),
                mimeType = cursor.getStringOrNull("mime_type"),
                normalizedSubjectHash = cursor.getLongOrNull("normalized_subject_hash"),
                empty = cursor.getIntOrNull("empty"),
                read = cursor.getIntOrNull("read"),
                flagged = cursor.getIntOrNull("flagged"),
                answered = cursor.getIntOrNull("answered"),
                forwarded = cursor.getIntOrNull("forwarded"),
                messagePartId = cursor.getLongOrNull("message_part_id"),
                encryptionType = cursor.getStringOrNull("encryption_type"),
                newMessage = cursor.getIntOrNull("new_message"),
                accountId = cursor.getStringOrNull("account_id"),
            )
        }
    }
}

data class MessageEntry(
    val id: Long?,
    val deleted: Int?,
    val folderId: Long?,
    val uid: String?,
    val subject: String?,
    val date: Long?,
    val flags: String?,
    val senderList: String?,
    val toList: String?,
    val ccList: String?,
    val bccList: String?,
    val replyToList: String?,
    val attachmentCount: Int?,
    val internalDate: Long?,
    val messageId: String?,
    val previewType: String?,
    val preview: String?,
    val mimeType: String?,
    val normalizedSubjectHash: Long?,
    val empty: Int?,
    val read: Int?,
    val flagged: Int?,
    val answered: Int?,
    val forwarded: Int?,
    val messagePartId: Long?,
    val encryptionType: String?,
    val newMessage: Int?,
    val accountId: String? = null,
)

fun SQLiteDatabase.createMessagePart(
    type: Int = 0,
    root: Int? = null,
    parent: Int = -1,
    seq: Int = 0,
    mimeType: String = "text/plain",
    decodedBodySize: Int = 0,
    displayName: String? = null,
    header: String? = null,
    encoding: String = "7bit",
    charset: String? = null,
    dataLocation: Int = 0,
    data: ByteArray? = null,
    preamble: String? = null,
    epilogue: String? = null,
    boundary: String? = null,
    contentId: String? = null,
    serverExtra: String? = null,
    directory: File? = null,
): Long {
    val values = ContentValues().apply {
        put("type", type)
        put("root", root)
        put("parent", parent)
        put("seq", seq)
        put("mime_type", mimeType)
        put("decoded_body_size", decodedBodySize)
        put("display_name", displayName)
        put("header", header?.toByteArray())
        put("encoding", encoding)
        put("charset", charset)
        put("data_location", dataLocation)
        put("data", data)
        put("preamble", preamble)
        put("epilogue", epilogue)
        put("boundary", boundary)
        put("content_id", contentId)
        put("server_extra", serverExtra)
    }

    return insert("message_parts", null, values).also { messagePartId ->
        if (dataLocation == DATA_LOCATION_ON_DISK) {
            requireNotNull(directory) { "Argument 'directory' can't be null when 'dataLocation = 2'" }
            File(directory, messagePartId.toString()).createNewFile()
        }
    }
}
