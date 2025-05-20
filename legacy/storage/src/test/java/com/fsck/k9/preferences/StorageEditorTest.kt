package com.fsck.k9.preferences

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperationCallback
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperations
import com.fsck.k9.storage.K9RobolectricTest
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.storage.Storage
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class DefaultStorageEditorTest : K9RobolectricTest() {
    private val storage: Storage =
        DefaultStorage(mapOf("storage-key" to "storage-value"))
    private val storageUpdater = TestStorageUpdater(storage)
    private val storagePersister = mock<K9StoragePersister>()
    private val storagePersisterOps = mock<StoragePersistOperations>()
    private val editor = K9StorageEditor(storageUpdater, storagePersister)

    private val workingMap = mutableMapOf<String, String>()

    private val newValues: Map<String, String>
        get() = storageUpdater.newStorage!!.getAll()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun commit_exception() {
        stubbing(storagePersister) {
            on { doInTransaction(any()) } doThrow RuntimeException()
        }

        val success = editor.commit()

        assertThat(success).isFalse()
    }

    @Test
    fun commit_trivial() {
        prepareStoragePersisterMock()

        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(storage.getAll())

        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putBoolean() {
        prepareStoragePersisterMock()

        editor.putBoolean("x", true)
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value", "x" to "true"))
        verify(storagePersisterOps).put("x", "true")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putInt() {
        prepareStoragePersisterMock()

        editor.putInt("x", 123)
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value", "x" to "123"))
        verify(storagePersisterOps).put("x", "123")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putLong() {
        prepareStoragePersisterMock()

        editor.putLong("x", 1234)
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value", "x" to "1234"))
        verify(storagePersisterOps).put("x", "1234")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString() {
        prepareStoragePersisterMock()

        editor.putString("x", "y")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value", "x" to "y"))
        verify(storagePersisterOps).put("x", "y")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_duplicateSame() {
        prepareStoragePersisterMock()

        editor.putString("storage-key", "storage-value")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value"))
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_duplicateOther() {
        prepareStoragePersisterMock()

        editor.putString("storage-key", "other-value")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "other-value"))
        verify(storagePersisterOps).put("storage-key", "other-value")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_removedDuplicate() {
        prepareStoragePersisterMock()

        editor.remove("storage-key")
        editor.putString("storage-key", "storage-value")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value"))
        verify(storagePersisterOps).remove("storage-key")
        verify(storagePersisterOps).put("storage-key", "storage-value")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun `remove key that doesn't exist`() {
        prepareStoragePersisterMock()

        editor.remove("x")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEqualTo(mapOf("storage-key" to "storage-value"))
        verify(storagePersisterOps).remove("x")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun remove() {
        prepareStoragePersisterMock()

        editor.remove("storage-key")
        val success = editor.commit()

        assertThat(success).isTrue()
        assertThat(newValues).isEmpty()
        verify(storagePersisterOps).remove("storage-key")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    private fun prepareStoragePersisterMock() {
        stubbing(storagePersisterOps) {
            on { put(any(), any()) } doAnswer {
                val key = it.getArgument<String>(0)
                val value = it.getArgument<String>(1)
                workingMap[key] = value
            }

            on { remove(any()) } doAnswer {
                val key = it.getArgument<String>(0)
                workingMap.remove(key)
                return@doAnswer
            }
        }

        stubbing(storagePersister) {
            on { doInTransaction(any()) } doAnswer {
                val operationCallback = it.getArgument<StoragePersistOperationCallback>(0)

                operationCallback.beforePersistTransaction(workingMap)
                operationCallback.persist(storagePersisterOps)
                operationCallback.onPersistTransactionSuccess(workingMap)
            }
        }
    }
}

class TestStorageUpdater(private val currentStorage: Storage) : StorageUpdater {
    var newStorage: Storage? = null

    override fun updateStorage(updater: (currentStorage: Storage) -> Storage) {
        newStorage = updater(currentStorage)
    }
}
