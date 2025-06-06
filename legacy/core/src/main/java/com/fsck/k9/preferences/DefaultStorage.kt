package com.fsck.k9.preferences

import java.util.Collections
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preferences.Storage

class DefaultStorage(
    values: Map<String, String>,
) : Storage {
    private val values: Map<String, String> = Collections.unmodifiableMap(values)

    override fun isEmpty(): Boolean = values.isEmpty()

    override fun contains(key: String): Boolean = values.contains(key)

    override fun getAll(): Map<String, String> = values

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key]
            ?.toBoolean()
            ?: defValue

    override fun getInt(key: String, defValue: Int): Int {
        val value = values[key] ?: return defValue
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            Log.e(e, "Could not parse int")
            defValue
        }
    }

    override fun getLong(key: String, defValue: Long): Long {
        val value = values[key] ?: return defValue
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            Log.e(e, "Could not parse long")
            defValue
        }
    }

    @Throws(NoSuchElementException::class)
    override fun getString(key: String): String =
        values.getValue(key)

    override fun getStringOrDefault(key: String, defValue: String): String =
        getStringOrNull(key) ?: defValue

    override fun getStringOrNull(key: String): String? =
        values[key]
}
