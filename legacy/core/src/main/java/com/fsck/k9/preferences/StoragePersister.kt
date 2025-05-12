package com.fsck.k9.preferences

import net.thunderbird.core.preferences.Storage

interface StoragePersister {
    fun loadValues(): Storage

    fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor
}

fun interface StorageUpdater {
    fun updateStorage(updater: (currentStorage: Storage) -> Storage)
}
