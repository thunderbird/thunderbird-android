package net.thunderbird.core.preference.storage

/**
 * Interface for editing the storage.
 *
 * This interface provides methods to put various types of values into the storage,
 * remove values, and commit the changes.
 */
interface StorageEditor {

    /**
     * Puts a boolean value into the storage.
     *
     * @param key The key for the value.
     * @param value The boolean value to put.
     * @return The StorageEditor instance for chaining.
     */
    fun putBoolean(key: String, value: Boolean): StorageEditor

    /**
     * Puts an integer value into the storage.
     *
     * @param key The key for the value.
     * @param value The integer value to put.
     * @return The StorageEditor instance for chaining.
     */
    fun putInt(key: String, value: Int): StorageEditor

    /**
     * Puts a long value into the storage.
     *
     * @param key The key for the value.
     * @param value The long value to put.
     * @return The StorageEditor instance for chaining.
     */
    fun putLong(key: String, value: Long): StorageEditor

    /**
     * Puts a string value into the storage.
     *
     * @param key The key for the value.
     * @param value The string value to put. If null, the key will be removed.
     * @return The StorageEditor instance for chaining.
     */
    fun putString(key: String, value: String?): StorageEditor

    /**
     * Removes a value from the storage.
     *
     * @param key The key for the value to remove.
     * @return The StorageEditor instance for chaining.
     */
    fun remove(key: String): StorageEditor

    /**
     * Commits the changes made to the storage.
     *
     * @return true if the commit was successful, false otherwise.
     */
    fun commit(): Boolean
}
