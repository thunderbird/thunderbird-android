package net.thunderbird.core.preference.storage

/**
 * Provides access to [Storage] and creates [StorageEditor] instances for updating preferences.
 */
interface StorageProvider {

    /**
     * The current [Storage] snapshot.
     *
     * It is loaded from disk once and updated whenever an editor created by [createStorageEditor] commits.
     */
    val storage: Storage

    /**
     * Creates a [StorageEditor] for updating preferences in storage.
     *
     * @return A new instance of [StorageEditor].
     */
    fun createStorageEditor(): StorageEditor
}
