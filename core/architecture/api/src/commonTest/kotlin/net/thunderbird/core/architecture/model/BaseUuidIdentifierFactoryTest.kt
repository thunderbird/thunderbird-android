package net.thunderbird.core.architecture.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private class TestId(value: Uuid) : BaseUuidIdentifier(value)

@OptIn(ExperimentalUuidApi::class)
private object TestIdFactory : BaseUuidIdentifierFactory<TestId>(::TestId)

@OptIn(ExperimentalUuidApi::class)
class BaseUuidIdentifierFactoryTest {

    @Test
    fun `given raw UUID when of is called then returns Id wrapping parsed UUID`() {
        // Arrange
        val raw = "123e4567-e89b-12d3-a456-426655440000"

        // Act
        val id = TestIdFactory.of(raw)

        // Assert
        assertThat(id.value).isEqualTo(Uuid.parse(raw))
        assertThat(id.toString()).isEqualTo(raw)
    }

    @Test
    fun `given create is called then returns UUIDv7 Id`() {
        // Arrange + Act
        val id = TestIdFactory.create()

        // Assert
        id.value.toLongs { mostSignificantBits, leastSignificantBits ->
            val version = (mostSignificantBits ushr 12) and 0x0F
            val variant = leastSignificantBits ushr 62

            assertThat(version).isEqualTo(7L)
            assertThat(variant).isEqualTo(2L)
        }
    }

    @Test
    fun `given create is called repeatedly then returns ordered unique Ids`() {
        // Arrange + Act
        val ids = List(100) { TestIdFactory.create() }

        // Assert
        assertThat(ids).isEqualTo(ids.sorted())
        assertThat(ids.toSet().size).isEqualTo(ids.size)
    }

    @Test
    fun `given existing UUID when of is called then returns compatible Id`() {
        // Arrange
        val raw = "123e4567-e89b-42d3-a456-426655440000"

        // Act
        val id = TestIdFactory.of(raw)

        // Assert
        assertThat(id.value).isEqualTo(Uuid.parse(raw))
    }

    @Test
    fun `given Id created when of is called with its raw then same Id is returned`() {
        // Arrange
        val original = TestIdFactory.create()
        val raw = original.toString()

        // Act
        val parsed = TestIdFactory.of(raw)

        // Assert
        assertThat(parsed).isEqualTo(original)
    }
}
