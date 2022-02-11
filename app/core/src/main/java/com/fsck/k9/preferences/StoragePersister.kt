package com.fsck.k9.preferences

interface StoragePersister {
    fun loadValues(): Storage

    fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor
}

fun interface StorageUpdater {
    fun updateStorage(updater: (currentStorage: Storage) -> Storage)
}
