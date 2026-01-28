package net.thunderbird.feature.mail.message.list.domain.model

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test

@Suppress("MaxLineLength")
class SortCriteriaTest {

    // region [Valid constructions]
    @Test
    fun `GIVEN primary sort is DateAsc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.DateAsc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertNull(criteria.secondary)
    }

    @Test
    fun `GIVEN primary sort is DateDesc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.DateDesc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertNull(criteria.secondary)
    }

    @Test
    fun `GIVEN primary sort is ArrivalAsc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.ArrivalAsc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertNull(criteria.secondary)
    }

    @Test
    fun `GIVEN primary sort is ArrivalDesc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.ArrivalDesc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertNull(criteria.secondary)
    }

    @Test
    fun `GIVEN primary sort requires secondary WHEN creating with DateAsc secondary THEN it is created`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.DateAsc

        // Act
        val criteria = SortCriteria(primary = primary, secondary = secondary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertEquals(secondary, criteria.secondary)
    }

    @Test
    fun `GIVEN primary sort requires secondary WHEN creating with DateDesc secondary THEN it is created`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.DateDesc

        // Act
        val criteria = SortCriteria(primary = primary, secondary = secondary)

        // Assert
        assertEquals(primary, criteria.primary)
        assertEquals(secondary, criteria.secondary)
    }

    // endregion [Valid constructions]

    // region [Invalid constructions]
    @Test
    fun `GIVEN primary is DateAsc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.DateAsc
        val secondary = SortType.DateDesc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = secondary)
        }
        assertEquals("Secondary sorting criterion must be null for $primary", exception.message)
    }

    @Test
    fun `GIVEN primary is DateDesc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.DateDesc
        val secondary = SortType.DateAsc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = secondary)
        }
        assertEquals("Secondary sorting criterion must be null for $primary", exception.message)
    }

    @Test
    fun `GIVEN primary is ArrivalAsc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.ArrivalAsc
        val secondary = SortType.DateDesc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = secondary)
        }
        assertEquals("Secondary sorting criterion must be null for $primary", exception.message)
    }

    @Test
    fun `GIVEN primary is ArrivalDesc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.ArrivalDesc
        val secondary = SortType.DateAsc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = secondary)
        }
        assertEquals("Secondary sorting criterion must be null for $primary", exception.message)
    }

    @Test
    fun `GIVEN primary requires secondary WHEN creating without secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.UnreadAsc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = null)
        }
        assertEquals("Secondary sorting criterion is missing for $primary", exception.message)
    }

    @Test
    fun `GIVEN primary requires secondary WHEN creating with unsupported secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.AttachmentDesc

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            SortCriteria(primary = primary, secondary = secondary)
        }
        assertEquals("Secondary sorting criterion $secondary is not supported for $primary", exception.message)
    }
    // endregion [Invalid constructions]
}
