package net.thunderbird.core.preference.storage

import androidx.annotation.GuardedBy

class DefaultStorageProvider(
    private val storagePersister: StoragePersister,
) : StorageProvider {

    private val storageLock = Any()

    @GuardedBy("storageLock")
    private var currentStorage: Storage? = null

    override val storage: Storage
        get() = synchronized(storageLock) {
            currentStorage ?: storagePersister.loadValues().also { newStorage ->
                currentStorage = newStorage
            }
        }

    override fun loadLatestStorage(): Storage = synchronized(storageLock) {
        storagePersister.loadValues().also { newStorage ->
            currentStorage = newStorage
        }
    }

    override fun createStorageEditor(): StorageEditor {
        return storagePersister.createStorageEditor { updater ->
            synchronized(storageLock) {
                currentStorage = updater(storage)
            }
        }
    }
}
