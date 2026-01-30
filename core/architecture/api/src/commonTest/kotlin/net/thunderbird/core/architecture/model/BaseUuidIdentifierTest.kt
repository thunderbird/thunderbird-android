package net.thunderbird.core.architecture.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BaseUuidIdentifierTest {

    private class TestId(value: Uuid) : BaseUuidIdentifier(value)

    @Test
    fun `given raw UUID when creating Id then toString returns same string and value matches`() {
        // Arrange
        val raw = "123e4567-e89b-12d3-a456-426655440000"
        val uuid = Uuid.parse(raw)

        // Act
        val id = TestId(uuid)

        // Assert
        assertThat(id.value).isEqualTo(uuid)
        assertThat(id.toString()).isEqualTo(raw)
    }

    @Test
    fun `given two Ids with same UUID then they are equal and have same hashCode`() {
        // Arrange
        val uuid = Uuid.parse("123e4567-e89b-12d3-a456-426655440000")

        // Act
        val id1 = TestId(uuid)
        val id2 = TestId(uuid)

        // Assert
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun `given two Ids with different UUIDs then they are not equal`() {
        // Arrange
        val id1 = TestId(Uuid.parse("123e4567-e89b-12d3-a456-426655440000"))
        val id2 = TestId(Uuid.parse("123e4567-e89b-12d3-a456-426655440001"))

        // Assert
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.toString()).isNotEqualTo(id2.toString())
    }

    @Test
    fun `toString returns the string representation of the UUID`() {
        // Arrange
        val raw = "123e4567-e89b-12d3-a456-426655440000"
        val uuid = Uuid.parse(raw)
        val id = TestId(uuid)

        // Act
        val toStringResult = id.toString()

        // Assert
        assertThat(toStringResult).isEqualTo(raw)
    }

    @Test
    fun `identifiers are comparable based on their value`() {
        // Arrange
        val uuid1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
        val uuid2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
        val id1 = TestId(uuid1)
        val id2 = TestId(uuid2)

        // Act & Assert
        assertThat(id1).isLessThan(id2)
        assertThat(id1.compareTo(id2)).isEqualTo(uuid1.compareTo(uuid2))
    }

    @Test
    fun `identifiers can be used in sorted collections`() {
        // Arrange
        val id1 = TestId(Uuid.parse("00000000-0000-0000-0000-000000000003"))
        val id2 = TestId(Uuid.parse("00000000-0000-0000-0000-000000000001"))
        val id3 = TestId(Uuid.parse("00000000-0000-0000-0000-000000000002"))

        val list = listOf(id1, id2, id3)

        // Act
        val sortedList = list.sorted()

        // Assert
        assertThat(sortedList).isEqualTo(listOf(id2, id3, id1))
    }
}
