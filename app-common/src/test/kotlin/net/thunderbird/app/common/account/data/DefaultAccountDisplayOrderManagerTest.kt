package net.thunderbird.app.common.account.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class DefaultAccountDisplayOrderManagerTest {

    private val fakeStorageProvider = FakeStorageProvider()

    @Test
    fun `moveToPosition should reorder account and commit to storage when account exists and new position is valid`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()
        val accountId3 = AccountIdFactory.create()

        val initialUuids = "$accountId1,$accountId2,$accountId3"
        val fakeStorage = FakeStorage(mapOf("accountUuids" to initialUuids))
        val testSubject = createTestSubject(fakeStorage)

        // Move accountId1 (from index 0) to index 2
        testSubject.moveToPosition(accountId1, 2)

        val expectedUuids = "$accountId2,$accountId3,$accountId1"
        assertThat(fakeStorageProvider.committedValues["accountUuids"]).isEqualTo(expectedUuids)
        assertThat(fakeStorageProvider.commitCalls).isEqualTo(1)
    }

    @Test
    fun `moveToPosition should handle moving account to position 0`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()
        val accountId3 = AccountIdFactory.create()

        val initialUuids = "$accountId1,$accountId2,$accountId3"
        val fakeStorage = FakeStorage(mapOf("accountUuids" to initialUuids))
        val testSubject = createTestSubject(fakeStorage)

        // Move accountId3 (from index 2) to index 0
        testSubject.moveToPosition(accountId3, 0)

        val expectedUuids = "$accountId3,$accountId1,$accountId2"
        assertThat(fakeStorageProvider.committedValues["accountUuids"]).isEqualTo(expectedUuids)
        assertThat(fakeStorageProvider.commitCalls).isEqualTo(1)
    }

    @Test
    fun `moveToPosition should do nothing when accountId is not in accountUuids`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()
        val missingAccountId = AccountIdFactory.create()

        val initialUuids = "$accountId1,$accountId2"
        val fakeStorage = FakeStorage(mapOf("accountUuids" to initialUuids))
        val testSubject = createTestSubject(fakeStorage)

        testSubject.moveToPosition(missingAccountId, 1)

        assertThat(fakeStorageProvider.committedValues["accountUuids"]).isNull()
        assertThat(fakeStorageProvider.commitCalls).isEqualTo(0)
    }

    @Test
    fun `moveToPosition should do nothing when newPosition equals current position`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()

        val initialUuids = "$accountId1,$accountId2"
        val fakeStorage = FakeStorage(mapOf("accountUuids" to initialUuids))
        val testSubject = createTestSubject(fakeStorage)

        // Move accountId1 to index 0 (its current index)
        testSubject.moveToPosition(accountId1, 0)

        assertThat(fakeStorageProvider.committedValues["accountUuids"]).isNull()
        assertThat(fakeStorageProvider.commitCalls).isEqualTo(0)
    }

    private fun createTestSubject(storage: Storage): DefaultAccountDisplayOrderManager {
        fakeStorageProvider.storageInstance = storage
        return DefaultAccountDisplayOrderManager(storageProvider = fakeStorageProvider)
    }

    private class FakeStorage(private val values: Map<String, String> = emptyMap()) : Storage {
        override fun isEmpty(): Boolean = values.isEmpty()
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun getAll(): Map<String, String> = values
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key]?.toBoolean() ?: defValue
        override fun getInt(key: String, defValue: Int): Int = values[key]?.toIntOrNull() ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key]?.toLongOrNull() ?: defValue
        override fun getString(key: String): String = values[key] ?: error("No value for $key")
        override fun getStringOrDefault(key: String, defValue: String): String = values[key] ?: defValue
        override fun getStringOrNull(key: String): String? = values[key]
    }

    private class FakeStorageEditor(
        private val storageProvider: FakeStorageProvider,
    ) : StorageEditor {
        val values = mutableMapOf<String, String?>()

        override fun putBoolean(key: String, value: Boolean): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putInt(key: String, value: Int): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putLong(key: String, value: Long): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putString(key: String, value: String?): StorageEditor {
            values[key] = value
            return this
        }

        override fun remove(key: String): StorageEditor {
            values.remove(key)
            return this
        }

        override fun commit(): Boolean {
            storageProvider.commitCalls++
            storageProvider.committedValues.putAll(values)
            return true
        }
    }

    private class FakeStorageProvider : StorageProvider {
        lateinit var storageInstance: Storage
        val committedValues = mutableMapOf<String, String?>()
        var commitCalls = 0

        override val storage: Storage get() = storageInstance
        override fun loadLatestStorage(): Storage = storageInstance
        override fun createStorageEditor(): StorageEditor = FakeStorageEditor(this)
    }
}
