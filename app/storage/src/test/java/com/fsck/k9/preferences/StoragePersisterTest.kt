package com.fsck.k9.preferences

import android.content.Context
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperationCallback
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperations
import com.fsck.k9.storage.K9RobolectricTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RuntimeEnvironment

class StoragePersisterTest : K9RobolectricTest() {
    private var context: Context = RuntimeEnvironment.application
    private var storagePersister = K9StoragePersister(context)

    @Test
    fun doInTransaction_order() {
        val operationCallback = prepareCallback()
        storagePersister.doInTransaction(operationCallback)

        inOrder(operationCallback) {
            verify(operationCallback).beforePersistTransaction(any())
            verify(operationCallback).persist(any())
            verify(operationCallback).onPersistTransactionSuccess(any())
        }
        verifyNoMoreInteractions(operationCallback)
    }

    @Test
    fun doInTransaction_put() {
        val operationCallback = prepareCallback(
            persistOp = { ops -> ops.put("x", "y") },
            onSuccess = { map ->
                assertEquals(1, map.size)
                assertEquals("y", map["x"])
            }
        )

        storagePersister.doInTransaction(operationCallback)

        val values = storagePersister.loadValues().all
        assertEquals(1, values.size)
        assertEquals("y", values["x"])
    }

    @Test
    fun doInTransaction_putAndThrow() {
        val exception = Exception("boom")
        val operationCallback = prepareCallback(
            persistOp = { ops ->
                ops.put("x", "y")
                throw exception
            }
        )

        try {
            storagePersister.doInTransaction(operationCallback)
            fail("expected exception")
        } catch (e: Exception) {
            assertSame(exception, e)
        }

        val values = storagePersister.loadValues()
        assertTrue(values.isEmpty())
        verify(operationCallback, never()).onPersistTransactionSuccess(any())
    }

    @Test
    fun doInTransaction_remove() {
        val operationCallback = prepareCallback(
            before = { map -> map["x"] = "y" },
            persistOp = { ops -> ops.remove("x") },
            onSuccess = { map -> assertTrue(map.isEmpty()) }
        )

        storagePersister.doInTransaction(operationCallback)

        val values = storagePersister.loadValues()
        assertTrue(values.isEmpty())
    }

    @Test
    fun doInTransaction_before_preserveButNotPersist() {
        val operationCallback = prepareCallback(
            before = { map -> map["x"] = "y" },
            onSuccess = { map -> assertEquals("y", map["x"]) }
        )

        storagePersister.doInTransaction(operationCallback)

        val values = storagePersister.loadValues()
        assertTrue(values.isEmpty())
    }

    private fun prepareCallback(
        persistOp: ((StoragePersistOperations) -> Unit)? = null,
        before: ((MutableMap<String, String>) -> Unit)? = null,
        onSuccess: ((Map<String, String>) -> Unit)? = null
    ): StoragePersistOperationCallback = spy(object : StoragePersistOperationCallback {
        override fun beforePersistTransaction(workingStorage: MutableMap<String, String>) {
            before?.invoke(workingStorage)
        }

        override fun persist(ops: StoragePersistOperations) {
            persistOp?.invoke(ops)
        }

        override fun onPersistTransactionSuccess(workingStorage: Map<String, String>) {
            onSuccess?.invoke(workingStorage)
        }
    })
}
