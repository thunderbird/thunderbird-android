package com.fsck.k9.preferences

import java.util.Collections
import net.thunderbird.core.preferences.Storage
import timber.log.Timber

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
            Timber.e(e, "Could not parse int")
            defValue
        }
    }

    override fun getLong(key: String, defValue: Long): Long {
        val value = values[key] ?: return defValue
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            Timber.e(e, "Could not parse long")
            defValue
        }
    }

    override fun getString(key: String?, defValue: String?): String =
        values[key] ?: defValue.orEmpty()
}
