package net.thunderbird.core.preference.storage

/**
 * Interface for updating the storage.
 */
fun interface StorageUpdater {

    /**
     * Updates the storage using the provided updater function.
     *
     * @param updater A function that takes the current storage and returns the updated storage.
     */
    fun updateStorage(updater: (currentStorage: Storage) -> Storage)
}
