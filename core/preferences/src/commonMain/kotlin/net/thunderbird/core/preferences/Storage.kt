package net.thunderbird.core.preferences

interface Storage {
    /**
     * Checks if the storage is empty.
     *
     * @return true if the storage is empty, false otherwise.
     */
    fun isEmpty(): Boolean

    /**
     * Checks if the storage contains a value for the given key.
     *
     * @param key The key to check.
     * @return true if the storage contains a value for the given key, false otherwise.
     */
    fun contains(key: String): Boolean

    /**
     * Returns a map of all key-value pairs in the storage.
     *
     * @return A map of all key-value pairs.
     */
    fun getAll(): Map<String, String>

    /**
     * Returns the boolean value for the given key.
     *
     * @param key The key to look up.
     * @param defValue The default value to return if the key is not found.
     * @return The boolean value for the given key, or the default value if the key is not found.
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean

    /**
     * Returns the integer value for the given key.
     *
     * @param key The key to look up.
     * @param defValue The default value to return if the key is not found.
     * @return The integer value for the given key, or the default value if the key is not found.
     */
    fun getInt(key: String, defValue: Int): Int

    /**
     * Returns the long value for the given key.
     *
     * @param key The key to look up.
     * @param defValue The default value to return if the key is not found.
     * @return The long value for the given key, or the default value if the key is not found.
     */
    fun getLong(key: String, defValue: Long): Long

    /**
     * Returns the string value for the given key.
     *
     * @param key The key to look up.
     * @return The string value for the given key.
     * @throws NoSuchElementException if the key is not found.
     */
    @Throws(NoSuchElementException::class)
    fun getString(key: String): String

    /**
     * Returns the string value for the given key.
     *
     * @param key The key to look up.
     * @param defValue The default value to return if the key is not found.
     * @return The string value for the given key, or the default value if the key is not found.
     */
    fun getStringOrDefault(key: String, defValue: String): String

    /**
     * Returns the string value for the given key, or null if the key is not found.
     *
     * @param key The key to look up.
     * @return The string value for the given key, or null if the key is not found.
     */
    fun getStringOrNull(key: String): String?
}

/**
 * Returns the enum value for the given key, or the default value if the key is not found or the stored value
 * is not a valid enum constant.
 *
 * @param T The enum type.
 * @param key The key to look up.
 * @param default The default enum value to return if the key is not found or the value is invalid.
 * @return The enum value for the given key, or the default value.
 * @throws IllegalArgumentException if the stored string value does not match any of the enum constants.
 */
@Throws(IllegalArgumentException::class)
inline fun <reified T : Enum<T>> Storage.getEnumOrDefault(key: String, default: T): T =
    getStringOrNull(key)
        ?.let { value ->
            try {
                enumValueOf<T>(value)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    buildString {
                        append("Unable to convert stored key [$key] value [$value] ")
                        appendLine("to enum of type ${T::class.qualifiedName}.")
                        append("Valid values: ${enumValues<T>().joinToString()}")
                    },
                    e,
                )
            }
        }
        ?: default
