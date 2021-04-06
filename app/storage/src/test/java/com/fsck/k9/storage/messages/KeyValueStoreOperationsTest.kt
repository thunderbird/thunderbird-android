package com.fsck.k9.storage.messages

import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyValueStoreOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val keyValueStoreOperations = KeyValueStoreOperations(lockableDatabase)

    @Test
    fun `get extra string`() {
        sqliteDatabase.createExtraValue(name = "test", text = "Wurstsalat")

        val result = keyValueStoreOperations.getExtraString("test")

        assertThat(result).isEqualTo("Wurstsalat")
    }

    @Test
    fun `get non-existent extra string`() {
        val result = keyValueStoreOperations.getExtraString("test")

        assertThat(result).isNull()
    }

    @Test
    fun `create extra string`() {
        keyValueStoreOperations.setExtraString("jmapState", "ABC42")

        val extraValues = sqliteDatabase.readExtraValues()
        assertThat(extraValues).hasSize(1)
        assertThat(extraValues.first()).isEqualTo(
            ExtraValueEntry(
                name = "jmapState",
                text = "ABC42",
                number = null
            )
        )
    }

    @Test
    fun `update extra string`() {
        sqliteDatabase.createExtraValue(name = "jmapState", text = "XYZ23")

        keyValueStoreOperations.setExtraString("jmapState", "ABC42")

        val extraValues = sqliteDatabase.readExtraValues()
        assertThat(extraValues).hasSize(1)
        assertThat(extraValues.first()).isEqualTo(
            ExtraValueEntry(
                name = "jmapState",
                text = "ABC42",
                number = null
            )
        )
    }

    @Test
    fun `get extra number`() {
        sqliteDatabase.createExtraValue(name = "test", number = 23)

        val result = keyValueStoreOperations.getExtraNumber("test")

        assertThat(result).isEqualTo(23)
    }

    @Test
    fun `get non-existent extra number`() {
        val result = keyValueStoreOperations.getExtraNumber("test")

        assertThat(result).isNull()
    }

    @Test
    fun `create extra number`() {
        keyValueStoreOperations.setExtraNumber("lastChanged", 123L)

        val extraValues = sqliteDatabase.readExtraValues()
        assertThat(extraValues).hasSize(1)
        assertThat(extraValues.first()).isEqualTo(
            ExtraValueEntry(
                name = "lastChanged",
                text = null,
                number = 123L
            )
        )
    }

    @Test
    fun `update extra number`() {
        sqliteDatabase.createExtraValue(name = "lastChanged", number = 0L)

        keyValueStoreOperations.setExtraNumber("lastChanged", 42L)

        val extraValues = sqliteDatabase.readExtraValues()
        assertThat(extraValues).hasSize(1)
        assertThat(extraValues.first()).isEqualTo(
            ExtraValueEntry(
                name = "lastChanged",
                text = null,
                number = 42
            )
        )
    }
}
