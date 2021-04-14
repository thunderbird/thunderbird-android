package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.helper.getIntOrNull
import com.fsck.k9.helper.getLongOrNull
import com.fsck.k9.helper.getStringOrNull
import com.fsck.k9.helper.map

fun SQLiteDatabase.createMessagePart(
    type: Int = MessagePartType.UNKNOWN,
    root: Int? = null,
    parent: Int = -1,
    seq: Int = 0,
    mimeType: String? = null,
    decodedBodySize: Int? = null,
    displayName: String? = null,
    header: String? = null,
    encoding: String? = null,
    charset: String? = null,
    dataLocation: Int = DataLocation.MISSING,
    data: ByteArray? = null,
    preamble: String? = null,
    epilogue: String? = null,
    boundary: String? = null,
    contentId: String? = null,
    serverExtra: String? = null
): Long {
    val values = ContentValues().apply {
        put("type", type)
        put("root", root)
        put("parent", parent)
        put("seq", seq)
        put("mime_type", mimeType)
        put("decoded_body_size", decodedBodySize)
        put("display_name", displayName)
        put("header", header)
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

    return insert("message_parts", null, values)
}

fun SQLiteDatabase.readMessageParts(): List<MessagePartEntry> {
    return rawQuery("SELECT * FROM message_parts", null).use { cursor ->
        cursor.map {
            MessagePartEntry(
                id = cursor.getLongOrNull("id"),
                type = cursor.getIntOrNull("type"),
                root = cursor.getLongOrNull("root"),
                parent = cursor.getLongOrNull("parent"),
                seq = cursor.getIntOrNull("seq"),
                mimeType = cursor.getStringOrNull("mime_type"),
                decodedBodySize = cursor.getIntOrNull("decoded_body_size"),
                displayName = cursor.getStringOrNull("display_name"),
                header = cursor.getBlobOrNull("header"),
                encoding = cursor.getStringOrNull("encoding"),
                charset = cursor.getStringOrNull("charset"),
                dataLocation = cursor.getIntOrNull("data_location"),
                data = cursor.getBlobOrNull("data"),
                preamble = cursor.getStringOrNull("preamble"),
                epilogue = cursor.getStringOrNull("epilogue"),
                boundary = cursor.getStringOrNull("boundary"),
                contentId = cursor.getStringOrNull("content_id"),
                serverExtra = cursor.getStringOrNull("server_extra"),
            )
        }
    }
}

class MessagePartEntry(
    val id: Long?,
    val type: Int?,
    val root: Long?,
    val parent: Long?,
    val seq: Int?,
    val mimeType: String?,
    val decodedBodySize: Int?,
    val displayName: String?,
    val header: ByteArray?,
    val encoding: String?,
    val charset: String?,
    val dataLocation: Int?,
    val data: ByteArray?,
    val preamble: String?,
    val epilogue: String?,
    val boundary: String?,
    val contentId: String?,
    val serverExtra: String?
)

private fun Cursor.getBlobOrNull(columnName: String): ByteArray? {
    val columnIndex = getColumnIndex(columnName)
    return if (isNull(columnIndex)) null else getBlob(columnIndex)
}
