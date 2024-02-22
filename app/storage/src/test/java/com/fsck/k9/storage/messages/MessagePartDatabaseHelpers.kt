package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getIntOrNull
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.getStringOrNull
import app.k9mail.core.android.common.database.map

fun SQLiteDatabase.createMessagePart(
    type: Int = MessagePartType.UNKNOWN,
    root: Long? = null,
    parent: Long = -1,
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
    serverExtra: String? = null,
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

data class MessagePartEntry(
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
    val serverExtra: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessagePartEntry

        if (id != other.id) return false
        if (type != other.type) return false
        if (root != other.root) return false
        if (parent != other.parent) return false
        if (seq != other.seq) return false
        if (mimeType != other.mimeType) return false
        if (decodedBodySize != other.decodedBodySize) return false
        if (displayName != other.displayName) return false
        if (header != null) {
            if (other.header == null) return false
            if (!header.contentEquals(other.header)) return false
        } else if (other.header != null) {
            return false
        }
        if (encoding != other.encoding) return false
        if (charset != other.charset) return false
        if (dataLocation != other.dataLocation) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) {
            return false
        }
        if (preamble != other.preamble) return false
        if (epilogue != other.epilogue) return false
        if (boundary != other.boundary) return false
        if (contentId != other.contentId) return false
        if (serverExtra != other.serverExtra) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (type ?: 0)
        result = 31 * result + (root?.hashCode() ?: 0)
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + (seq ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (decodedBodySize ?: 0)
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (header?.contentHashCode() ?: 0)
        result = 31 * result + (encoding?.hashCode() ?: 0)
        result = 31 * result + (charset?.hashCode() ?: 0)
        result = 31 * result + (dataLocation ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (preamble?.hashCode() ?: 0)
        result = 31 * result + (epilogue?.hashCode() ?: 0)
        result = 31 * result + (boundary?.hashCode() ?: 0)
        result = 31 * result + (contentId?.hashCode() ?: 0)
        result = 31 * result + (serverExtra?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "MessagePartEntry(" +
            "id=$id, " +
            "type=$type, " +
            "root=$root, " +
            "parent=$parent, " +
            "seq=$seq, " +
            "mimeType=$mimeType, " +
            "decodedBodySize=$decodedBodySize, " +
            "displayName=$displayName, " +
            "header=${header?.decodeToString()}, " +
            "encoding=$encoding, " +
            "charset=$charset, " +
            "dataLocation=$dataLocation, " +
            "data=${data?.decodeToString()}, " +
            "preamble=$preamble, " +
            "epilogue=$epilogue, " +
            "boundary=$boundary, " +
            "contentId=$contentId, " +
            "serverExtra=$serverExtra)"
    }
}

private fun Cursor.getBlobOrNull(columnName: String): ByteArray? {
    val columnIndex = getColumnIndex(columnName)
    return if (isNull(columnIndex)) null else getBlob(columnIndex)
}
