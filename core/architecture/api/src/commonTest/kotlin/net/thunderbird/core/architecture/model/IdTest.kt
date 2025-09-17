package net.thunderbird.core.architecture.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class IdTestTag

@OptIn(ExperimentalUuidApi::class)
class IdTest {

    @Test
    fun `given raw UUID when creating Id then asRaw returns same string and value matches`() {
        // Arrange
        val raw = "123e4567-e89b-12d3-a456-426655440000"
        val uuid = Uuid.parse(raw)

        // Act
        val id = Id<IdTestTag>(uuid)

        // Assert
        assertThat(id.value).isEqualTo(uuid)
        assertThat(id.asRaw()).isEqualTo(raw)
    }

    @Test
    fun `given two Ids with same UUID then they are equal and have same hashCode`() {
        // Arrange
        val uuid = Uuid.parse("123e4567-e89b-12d3-a456-426655440000")

        // Act
        val id1 = Id<IdTestTag>(uuid)
        val id2 = Id<IdTestTag>(uuid)

        // Assert
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun `given two Ids with different UUIDs then they are not equal`() {
        // Arrange
        val id1 = Id<IdTestTag>(Uuid.parse("123e4567-e89b-12d3-a456-426655440000"))
        val id2 = Id<IdTestTag>(Uuid.parse("123e4567-e89b-12d3-a456-426655440001"))

        // Assert
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.asRaw()).isNotEqualTo(id2.asRaw())
    }
}
