package net.thunderbird.core.preferences

interface Storage {
    fun isEmpty(): Boolean

    fun contains(key: String): Boolean

    fun getAll(): Map<String, String>

    fun getBoolean(key: String, defValue: Boolean): Boolean

    fun getInt(key: String, defValue: Int): Int

    fun getLong(key: String, defValue: Long): Long

    fun getString(key: String?, defValue: String?): String
}
