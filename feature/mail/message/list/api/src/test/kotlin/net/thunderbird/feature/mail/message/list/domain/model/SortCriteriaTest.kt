package net.thunderbird.feature.mail.message.list.domain.model

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
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
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isNull()
    }

    @Test
    fun `GIVEN primary sort is DateDesc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.DateDesc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isNull()
    }

    @Test
    fun `GIVEN primary sort is ArrivalAsc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.ArrivalAsc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isNull()
    }

    @Test
    fun `GIVEN primary sort is ArrivalDesc WHEN creating SortCriteria THEN it is created without secondary`() {
        // Arrange
        val primary = SortType.ArrivalDesc

        // Act
        val criteria = SortCriteria(primary = primary)

        // Assert
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isNull()
    }

    @Test
    fun `GIVEN primary sort requires secondary WHEN creating with DateAsc secondary THEN it is created`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.DateAsc

        // Act
        val criteria = SortCriteria(primary = primary, secondary = secondary)

        // Assert
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isEqualTo(secondary)
    }

    @Test
    fun `GIVEN primary sort requires secondary WHEN creating with DateDesc secondary THEN it is created`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.DateDesc

        // Act
        val criteria = SortCriteria(primary = primary, secondary = secondary)

        // Assert
        assertThat(criteria.primary).isEqualTo(primary)
        assertThat(criteria.secondary).isEqualTo(secondary)
    }

    // endregion [Valid constructions]

    // region [Invalid constructions]
    @Test
    fun `GIVEN primary is DateAsc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.DateAsc
        val secondary = SortType.DateDesc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = secondary)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion must be null for $primary")
    }

    @Test
    fun `GIVEN primary is DateDesc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.DateDesc
        val secondary = SortType.DateAsc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = secondary)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion must be null for $primary")
    }

    @Test
    fun `GIVEN primary is ArrivalAsc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.ArrivalAsc
        val secondary = SortType.DateDesc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = secondary)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion must be null for $primary")
    }

    @Test
    fun `GIVEN primary is ArrivalDesc WHEN creating with secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.ArrivalDesc
        val secondary = SortType.DateAsc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = secondary)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion must be null for $primary")
    }

    @Test
    fun `GIVEN primary requires secondary WHEN creating without secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.UnreadAsc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = null)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion is missing for $primary")
    }

    @Test
    fun `GIVEN primary requires secondary WHEN creating with unsupported secondary THEN it throws IllegalArgumentException`() {
        // Arrange
        val primary = SortType.UnreadAsc
        val secondary = SortType.AttachmentDesc

        // Act & Assert
        assertFailure {
            SortCriteria(primary = primary, secondary = secondary)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Secondary sorting criterion $secondary is not supported for $primary")
    }
    // endregion [Invalid constructions]
}
