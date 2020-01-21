package com.fsck.k9.preferences

import com.fsck.k9.storage.K9RobolectricTest
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class StorageEditorTest : K9RobolectricTest() {
    @Mock private lateinit var storage: Storage
    @Mock private lateinit var storagePersister: K9StoragePersister
    @Mock private lateinit var storagePersisterOps: K9StoragePersister.StoragePersistOperations
    private lateinit var editor: K9StorageEditor

    private val workingMap = mutableMapOf<String, String>()
    private val storageMap = mapOf(
            "storage-key" to "storage-value"
    )

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(storage.all).thenReturn(storageMap)

        editor = K9StorageEditor(storage, storagePersister)
        verify(storage).all
    }

    @Test
    fun commit_exception() {
        whenever(storagePersister.doInTransaction(any())).thenThrow(RuntimeException())

        val success = editor.commit()

        assertFalse(success)
    }

    @Test
    fun commit_trivial() {
        prepareStoragePersisterMock()

        val success = editor.commit()

        assertTrue(success)
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putBoolean() {
        prepareStoragePersisterMock()

        editor.putBoolean("x", true)
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).put("x", "true")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putInt() {
        prepareStoragePersisterMock()

        editor.putInt("x", 123)
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).put("x", "123")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putLong() {
        prepareStoragePersisterMock()

        editor.putLong("x", 1234)
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).put("x", "1234")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString() {
        prepareStoragePersisterMock()

        editor.putString("x", "y")
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).put("x", "y")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_duplicateSame() {
        prepareStoragePersisterMock()

        editor.putString("storage-key", "storage-value")
        val success = editor.commit()

        assertTrue(success)
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_duplicateOther() {
        prepareStoragePersisterMock()

        editor.putString("storage-key", "other-value")
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).put("storage-key", "other-value")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun putString_removedDuplicate() {
        prepareStoragePersisterMock()

        editor.remove("storage-key")
        editor.putString("storage-key", "storage-value")
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).remove("storage-key")
        verify(storagePersisterOps).put("storage-key", "storage-value")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    @Test
    fun remove() {
        prepareStoragePersisterMock()

        editor.remove("x")
        val success = editor.commit()

        assertTrue(success)
        verify(storagePersisterOps).remove("x")
        verifyNoMoreInteractions(storagePersisterOps)
    }

    private fun prepareStoragePersisterMock() {
        whenever(storagePersisterOps.put(any(), any())).then {
            val key = it.getArgument<String>(0)
            val value = it.getArgument<String>(0)

            workingMap[key] = value
            Unit
        }
        whenever(storagePersisterOps.remove(any())).then {
            val key = it.getArgument<String>(0)
            val value = it.getArgument<String>(0)

            workingMap[key] = value
            Unit
        }

        whenever(storagePersister.doInTransaction(any())).then {
            val operationCallback = it.getArgument<K9StoragePersister.StoragePersistOperationCallback>(0)
            operationCallback.beforePersistTransaction(workingMap)
            verify(storage, times(2)).all
            assertEquals(workingMap, storageMap)

            operationCallback.persist(storagePersisterOps)
            verify(storagePersister).doInTransaction(any())

            operationCallback.onPersistTransactionSuccess(workingMap)
            verify(storage).replaceAll(eq(workingMap))
        }
    }
}
