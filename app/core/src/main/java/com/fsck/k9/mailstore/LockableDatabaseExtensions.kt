package com.fsck.k9.mailstore

import android.database.Cursor

internal fun <T> LockableDatabase.query(
    table: String,
    columns: Array<String>,
    selection: String?,
    vararg selectionArgs: String,
    block: (Cursor) -> T
): T {
    return execute(false) { db ->
        val cursor = db.query(table, columns, selection, selectionArgs, null, null, null)
        cursor.use(block)
    }
}

internal fun <T> LockableDatabase.rawQuery(sql: String, vararg selectionArgs: String, block: (Cursor) -> T): T {
    return execute(false) { db ->
        val cursor = db.rawQuery(sql, selectionArgs)
        cursor.use(block)
    }
}
