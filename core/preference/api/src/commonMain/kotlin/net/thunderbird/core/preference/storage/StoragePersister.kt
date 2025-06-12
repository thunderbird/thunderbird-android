package net.thunderbird.core.preference.storage

/**
 * Represents a mechanism for persisting storage data.
 *
 * This interface provides methods to:
 * - Load the current storage values.
 * - Create a storage editor for applying updates to the storage.
 */
interface StoragePersister {

    /**
     * Loads the storage values.
     *
     * @return The loaded storage.
     */
    fun loadValues(): Storage

    /**
     * Creates a storage editor for updating the storage.
     *
     * @param storageUpdater The updater to apply changes to the storage.
     * @return A new instance of [StorageEditor].
     */
    fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor
}
