package app.k9mail.core.android.common.database

import android.database.Cursor

fun Cursor.getStringValue(key: String): String? {
    val columnIndex = getColumnIndex(key)
    return getString(columnIndex)
}

fun Cursor.getLongValue(key: String): Long {
    val columnIndex = getColumnIndex(key)
    return getLong(columnIndex)
}
