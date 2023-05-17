package app.k9mail.core.android.common.database

import android.database.Cursor
import android.database.MatrixCursor
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

data class CursorExtensionsAccessTestData<T : Any>(
    val name: String,
    val value: T,
    val access: (Cursor, String) -> T?,
    val throwingAccess: (Cursor, String) -> T,
) {
    override fun toString(): String = name
}

@RunWith(ParameterizedRobolectricTestRunner::class)
class CursorExtensionsKtAccessTest(data: CursorExtensionsAccessTestData<Any>) {

    private val testValue = data.value
    private val testAction = data.access
    private val testThrowingAction = data.throwingAccess

    @Test
    fun `testAction should return null if column is null`() {
        val cursor = MatrixCursor(arrayOf("column")).apply {
            addRow(arrayOf(null))
        }

        val result = cursor.map { testAction(it, "column") }

        assertThat(result[0]).isNull()
    }

    @Test
    fun `testAction should return value if column is not null`() {
        val cursor = MatrixCursor(arrayOf("column")).apply {
            addRow(arrayOf(testValue))
        }

        val result = cursor.map { testAction(it, "column") }

        assertThat(result[0]).isEqualTo(testValue)
    }

    @Test
    fun `testThrowingAction should throw if column is null`() {
        val cursor = MatrixCursor(arrayOf("column")).apply {
            addRow(arrayOf(null))
        }

        assertFailure {
            cursor.map { testThrowingAction(it, "column") }
        }.hasMessage("Column column must not be null")
    }

    @Test
    fun `testThrowingAction should return value if column is not null`() {
        val cursor = MatrixCursor(arrayOf("column")).apply {
            addRow(arrayOf(testValue))
        }

        val result = cursor.map { testThrowingAction(it, "column") }

        assertThat(result[0]).isEqualTo(testValue)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): Collection<CursorExtensionsAccessTestData<Any>> {
            return listOf(
                CursorExtensionsAccessTestData(
                    name = "getString",
                    value = "value",
                    access = { cursor, column -> cursor.getStringOrNull(column) },
                    throwingAccess = { cursor, column -> cursor.getStringOrThrow(column) },
                ),
                CursorExtensionsAccessTestData(
                    name = "getInt",
                    value = Int.MAX_VALUE,
                    access = { cursor, column -> cursor.getIntOrNull(column) },
                    throwingAccess = { cursor, column -> cursor.getIntOrThrow(column) },
                ),
                CursorExtensionsAccessTestData(
                    name = "getLong",
                    value = Long.MAX_VALUE,
                    access = { cursor, column -> cursor.getLongOrNull(column) },
                    throwingAccess = { cursor, column -> cursor.getLongOrThrow(column) },
                ),
            )
        }
    }
}
