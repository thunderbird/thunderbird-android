package net.thunderbird.core.preference.storage

import net.thunderbird.core.logging.Logger

class InMemoryStorage(
    private val values: Map<String, String>,
    private val logger: Logger,
) : Storage {

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
            logger.error(
                message = { "Could not parse int" },
                throwable = e,
            )
            defValue
        }
    }

    override fun getLong(key: String, defValue: Long): Long {
        val value = values[key] ?: return defValue
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            logger.error(
                message = { "Could not parse long" },
                throwable = e,
            )
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
