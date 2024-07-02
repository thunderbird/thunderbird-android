package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.getStringOrNull
import app.k9mail.core.android.common.database.map

fun SQLiteDatabase.createExtraValue(
    name: String = "irrelevant",
    text: String? = null,
    number: Long? = null,
): Long {
    val values = ContentValues().apply {
        put("name", name)
        put("value_text", text)
        put("value_integer", number)
    }

    return insert("account_extra_values", null, values)
}

fun SQLiteDatabase.readExtraValues(): List<ExtraValueEntry> {
    val cursor = rawQuery("SELECT * FROM account_extra_values", null)
    return cursor.use {
        cursor.map {
            ExtraValueEntry(
                name = cursor.getStringOrNull("name"),
                text = cursor.getStringOrNull("value_text"),
                number = cursor.getLongOrNull("value_integer"),
            )
        }
    }
}

data class ExtraValueEntry(
    val name: String?,
    val text: String?,
    val number: Long?,
)

fun SQLiteDatabase.createFolderExtraValue(
    folderId: Long,
    name: String = "irrelevant",
    text: String? = null,
    number: Long? = null,
): Long {
    val values = ContentValues().apply {
        put("folder_id", folderId)
        put("name", name)
        put("value_text", text)
        put("value_integer", number)
    }

    return insert("folder_extra_values", null, values)
}

fun SQLiteDatabase.readFolderExtraValues(): List<FolderExtraValueEntry> {
    val cursor = rawQuery("SELECT * FROM folder_extra_values", null)
    return cursor.use {
        cursor.map {
            FolderExtraValueEntry(
                folderId = cursor.getLongOrNull("folder_id"),
                name = cursor.getStringOrNull("name"),
                text = cursor.getStringOrNull("value_text"),
                number = cursor.getLongOrNull("value_integer"),
            )
        }
    }
}

data class FolderExtraValueEntry(
    val folderId: Long?,
    val name: String?,
    val text: String?,
    val number: Long?,
)
