package com.fsck.k9.helper

import android.database.Cursor

fun <T> Cursor.map(block: (Cursor) -> T): List<T> {
    return List(count) { index ->
        moveToPosition(index)
        block(this)
    }
}
