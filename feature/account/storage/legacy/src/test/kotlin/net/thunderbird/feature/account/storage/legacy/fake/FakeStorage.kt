package net.thunderbird.feature.account.storage.legacy.fake

import net.thunderbird.core.preference.storage.Storage

class FakeStorage(private val values: Map<String, String> = emptyMap()) : Storage {
    override fun isEmpty(): Boolean = values.isEmpty()

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun getAll(): Map<String, String> = values

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key]?.toBoolean() ?: defValue

    override fun getInt(key: String, defValue: Int): Int =
        values[key]?.toIntOrNull() ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        values[key]?.toLongOrNull() ?: defValue

    override fun getString(key: String): String =
        values[key] ?: throw NoSuchElementException("No value for key $key")

    override fun getStringOrDefault(key: String, defValue: String): String =
        values[key] ?: defValue

    override fun getStringOrNull(key: String): String? = values[key]
}
