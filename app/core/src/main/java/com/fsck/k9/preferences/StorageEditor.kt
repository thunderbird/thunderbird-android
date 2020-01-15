package com.fsck.k9.preferences

import android.content.SharedPreferences

interface StorageEditor {
    fun copy(input: SharedPreferences)

    fun putBoolean(key: String, value: Boolean): StorageEditor
    fun putInt(key: String, value: Int): StorageEditor
    fun putLong(key: String, value: Long): StorageEditor
    fun putString(key: String, value: String?): StorageEditor

    fun remove(key: String): StorageEditor

    fun commit(): Boolean
}
