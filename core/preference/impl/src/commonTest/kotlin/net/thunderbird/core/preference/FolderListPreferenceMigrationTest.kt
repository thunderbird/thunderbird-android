package net.thunderbird.core.preference

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

class FolderListPreferenceMigrationTest {
    private val OLD_KEY = "account_setup_auto_expand_folder"
    private val NEW_KEY = "auto_select_folder"

    @Test
    fun `apply should migrate value when OLD_KEY exists`() {
        // Given
        val storage = FakeStorage().apply {
            setString(OLD_KEY, "inbox")
        }
        val editor = FakeStorageEditor(storage)

        val migration = FolderListPreferenceMigration(storage, editor)

        // When
        migration.apply()
        editor.commit()

        // Then
        assertThat(storage.contains(NEW_KEY)).isTrue()
        assertThat(storage.getString(NEW_KEY)).isEqualTo("inbox")

        assertThat(storage.contains(OLD_KEY)).isFalse()
    }

    @Test
    fun `apply should do nothing when OLD_KEY does not exist`() {
        // Given
        val storage = FakeStorage()
        val editor = FakeStorageEditor(storage)
        val migration = FolderListPreferenceMigration(storage, editor)

        // When
        migration.apply()
        editor.commit()

        // Then
        assertThat(storage.contains(NEW_KEY)).isFalse()
        assertThat(storage.contains(OLD_KEY)).isFalse()
    }
}

class FakeStorage : Storage {

    private val map = mutableMapOf<String, Any?>()

    internal fun applyChanges(changes: Map<String, Any?>, removals: Set<String>) {
        removals.forEach { map.remove(it) }
        changes.forEach { (k, v) -> map[k] = v }
    }

    override fun isEmpty(): Boolean {
        error("Not Implemented")
    }

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun getAll(): Map<String, String> {
        error("Not Implemented")
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        error("Not Implemented")
    }

    override fun getInt(key: String, defValue: Int): Int {
        error("Not Implemented")
    }

    override fun getLong(key: String, defValue: Long): Long {
        error("Not Implemented")
    }

    override fun getString(key: String): String =
        map[key] as String? ?: throw NoSuchElementException()

    override fun getStringOrDefault(key: String, defValue: String): String {
        error("Not Implemented")
    }

    override fun getStringOrNull(key: String): String? {
        error("Not Implemented")
    }

    fun setString(key: String, value: String?) {
        map[key] = value
    }

    fun remove(key: String) {
        map.remove(key)
    }
}

class FakeStorageEditor(
    private val storage: FakeStorage,
) : StorageEditor {

    private val pendingChanges = mutableMapOf<String, Any?>()
    private val pendingRemovals = mutableSetOf<String>()

    override fun putBoolean(key: String, value: Boolean): StorageEditor {
        pendingChanges[key] = value
        return this
    }

    override fun putInt(key: String, value: Int): StorageEditor {
        pendingChanges[key] = value
        return this
    }

    override fun putLong(key: String, value: Long): StorageEditor {
        pendingChanges[key] = value
        return this
    }

    override fun putString(key: String, value: String?): StorageEditor {
        pendingChanges[key] = value
        return this
    }

    override fun remove(key: String): StorageEditor {
        pendingRemovals.add(key)
        return this
    }

    override fun commit(): Boolean {
        storage.applyChanges(pendingChanges, pendingRemovals)
        pendingChanges.clear()
        pendingRemovals.clear()
        return true
    }
}
