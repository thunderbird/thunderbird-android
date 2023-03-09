package app.k9mail.core.android.common.database

import android.database.MatrixCursor
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CursorExtensionsKtTest {

    @Test
    fun `map should return an empty list if cursor is empty`() {
        val cursor = MatrixCursor(arrayOf("column"))

        val result = cursor.map { it.getStringOrNull("column") }

        assertThat(result).isEqualTo(emptyList<String>())
    }

    @Test
    fun `map should return a list of mapped values`() {
        val cursor = MatrixCursor(arrayOf("column")).apply {
            addRow(arrayOf("value1"))
            addRow(arrayOf("value2"))
        }

        val result = cursor.map { it.getStringOrNull("column") }

        assertThat(result).isEqualTo(listOf("value1", "value2"))
    }
}
