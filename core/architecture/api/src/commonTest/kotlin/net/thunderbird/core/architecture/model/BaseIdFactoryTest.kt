package net.thunderbird.core.architecture.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private class TestTag

@OptIn(ExperimentalUuidApi::class)
private object TestIdFactory : BaseIdFactory<TestTag>()

@OptIn(ExperimentalUuidApi::class)
class BaseIdFactoryTest {

    @Test
    fun `given raw UUID when of is called then returns Id wrapping parsed UUID`() {
        // Arrange
        val raw = "123e4567-e89b-12d3-a456-426655440000"

        // Act
        val id = TestIdFactory.of(raw)

        // Assert
        assertThat(id.value).isEqualTo(Uuid.parse(raw))
        assertThat(id.asRaw()).isEqualTo(raw)
    }

    @Test
    fun `given create is called twice then returns different Ids`() {
        // Arrange + Act
        val id1 = TestIdFactory.create()
        val id2 = TestIdFactory.create()

        // Assert
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.asRaw()).isNotEqualTo(id2.asRaw())
    }

    @Test
    fun `given Id created when of is called with its raw then same Id is returned`() {
        // Arrange
        val original = TestIdFactory.create()
        val raw = original.asRaw()

        // Act
        val parsed = TestIdFactory.of(raw)

        // Assert
        assertThat(parsed).isEqualTo(Id<TestTag>(Uuid.parse(raw)))
        assertThat(parsed).isEqualTo(original)
    }
}
