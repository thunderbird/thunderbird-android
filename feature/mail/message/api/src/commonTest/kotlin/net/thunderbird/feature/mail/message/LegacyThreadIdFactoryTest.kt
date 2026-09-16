package net.thunderbird.feature.mail.message

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LegacyThreadIdFactoryTest {

    @Test
    fun `given legacy id when of is called then encodes it with the THREADS byte representation`() {
        // Arrange
        val legacyId = 12345L

        // Act
        val threadId = LegacyThreadIdFactory.of(legacyId)

        // Assert
        assertThat(threadId.toString()).isEqualTo("d3adc0d3-0000-8200-8000-000000003039")
    }

    @Test
    fun `given ThreadId created by of when toLegacyId is called then returns the original legacy id`() {
        // Arrange
        val legacyIds = listOf(0L, 1L, 42L, 65_536L, 4_294_967_296L, 281_474_976_710_656L, Long.MAX_VALUE)

        legacyIds.forEach { legacyId ->
            // Act
            val result = LegacyThreadIdFactory.toLegacyId(LegacyThreadIdFactory.of(legacyId))

            // Assert
            assertThat(result).isEqualTo(legacyId)
        }
    }

    @Test
    fun `given ThreadId created by of when isLegacy is called then returns true`() {
        // Arrange
        val threadId = LegacyThreadIdFactory.of(legacyId = 1L)

        // Act
        val result = LegacyThreadIdFactory.isLegacy(threadId)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `given ThreadId with random UUID when isLegacy is called then returns false`() {
        // Arrange
        val threadId = ThreadId(Uuid.random())

        // Act
        val result = LegacyThreadIdFactory.isLegacy(threadId)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `given negative legacy id when of is called then throws IllegalArgumentException`() {
        // Arrange
        val legacyId = -1L

        // Act & Assert
        assertFailure { LegacyThreadIdFactory.of(legacyId) }.isInstanceOf<IllegalArgumentException>()
    }
}
