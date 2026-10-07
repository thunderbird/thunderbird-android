package net.thunderbird.core.preference.storage

/**
 * Provides access to [Storage] and creates [StorageEditor] instances for updating preferences.
 */
interface StorageProvider {

    /**
     * The current [Storage] snapshot.
     */
    val storage: Storage

    /**
     * Loads the latest [Storage] from disk.
     *
     * @return The latest loaded [Storage].
     */
    fun loadLatestStorage(): Storage

    /**
     * Creates a [StorageEditor] for updating preferences in storage.
     *
     * @return A new instance of [StorageEditor].
     */
    fun createStorageEditor(): StorageEditor
}
