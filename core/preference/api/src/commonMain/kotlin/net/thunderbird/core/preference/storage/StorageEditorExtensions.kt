package net.thunderbird.core.preference.storage

/**
 * Extension functions for the [StorageEditor] interface to simplify putting enum values.
 *
 * @param T The type of the enum.
 * @param key The key under which the enum value will be stored.
 * @param value The enum value to be stored.
 */
inline fun <reified T : Enum<T>> StorageEditor.putEnum(key: String, value: T) {
    putString(key, value.name)
}
