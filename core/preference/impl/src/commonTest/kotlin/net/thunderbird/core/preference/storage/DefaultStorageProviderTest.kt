package net.thunderbird.core.preference.storage

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.logging.testing.TestLogger

class DefaultStorageProviderTest {

    private val logger = TestLogger()

    @Test
    fun `storage should load values from storagePersister`() {
        val storageMap = mapOf("key1" to "value1")
        val storagePersister = object : StoragePersister {
            override fun loadValues(): Storage = InMemoryStorage(storageMap, logger)
            override fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor {
                error("Not implemented")
            }
        }
        val provider = DefaultStorageProvider(storagePersister)

        assertThat(provider.storage.getStringOrDefault("key1", "")).isEqualTo("value1")
    }

    @Test
    fun `createStorageEditor should update currentStorage`() {
        val currentMap = mapOf("key1" to "value1")
        val storagePersister = object : StoragePersister {
            override fun loadValues(): Storage = InMemoryStorage(currentMap, logger)
            override fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor {
                return object : StorageEditor {
                    override fun commit(): Boolean {
                        storageUpdater.updateStorage { current ->
                            val updatedValues = current.getAll() + ("key2" to "value2")
                            InMemoryStorage(updatedValues, logger)
                        }
                        return true
                    }
                    override fun putBoolean(key: String, value: Boolean): StorageEditor = this
                    override fun putInt(key: String, value: Int): StorageEditor = this
                    override fun putLong(key: String, value: Long): StorageEditor = this
                    override fun putString(key: String, value: String?): StorageEditor = this
                    override fun remove(key: String): StorageEditor = this
                }
            }
        }
        val provider = DefaultStorageProvider(storagePersister)

        val editor = provider.createStorageEditor()
        editor.commit()

        assertThat(provider.storage.getStringOrDefault("key1", "")).isEqualTo("value1")
    }
}
