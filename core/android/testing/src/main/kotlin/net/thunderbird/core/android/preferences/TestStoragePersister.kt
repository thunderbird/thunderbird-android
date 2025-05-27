package net.thunderbird.core.android.preferences

import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.InMemoryStorage
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.StorageUpdater

class TestStoragePersister(
    private val logger: Logger,
) : StoragePersister {
    private val values = mutableMapOf<String, Any?>()

    override fun loadValues(): Storage {
        return InMemoryStorage(
            values = values.mapValues { (_, value) ->
                value?.toString() ?: ""
            },
            logger,
        )
    }

    override fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor {
        return InMemoryStorageEditor(storageUpdater)
    }

    private inner class InMemoryStorageEditor(private val storageUpdater: StorageUpdater) : StorageEditor {
        private val removals = mutableSetOf<String>()
        private val changes = mutableMapOf<String, String>()
        private var alreadyCommitted = false

        override fun putBoolean(key: String, value: Boolean) = apply {
            changes[key] = value.toString()
            removals.remove(key)
        }

        override fun putInt(key: String, value: Int) = apply {
            changes[key] = value.toString()
            removals.remove(key)
        }

        override fun putLong(key: String, value: Long) = apply {
            changes[key] = value.toString()
            removals.remove(key)
        }

        override fun putString(key: String, value: String?) = apply {
            if (value == null) {
                remove(key)
            } else {
                changes[key] = value
                removals.remove(key)
            }
        }

        override fun remove(key: String) = apply {
            removals.add(key)
            changes.remove(key)
        }

        override fun commit(): Boolean {
            if (alreadyCommitted) throw AssertionError("StorageEditor.commit() called more than once")
            alreadyCommitted = true

            storageUpdater.updateStorage(::writeValues)

            return true
        }

        private fun writeValues(currentStorage: Storage): Storage {
            return InMemoryStorage(currentStorage.getAll() - removals + changes, logger)
        }
    }
}
