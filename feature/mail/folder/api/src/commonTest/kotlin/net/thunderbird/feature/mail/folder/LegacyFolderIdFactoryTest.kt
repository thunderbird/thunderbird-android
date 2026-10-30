package net.thunderbird.feature.mail.folder

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
class LegacyFolderIdFactoryTest {

    @Test
    fun `given legacy id when of is called then encodes it with the FOLDERS byte representation`() {
        // Arrange
        val legacyId = 12345L

        // Act
        val folderId = LegacyFolderIdFactory.of(legacyId)

        // Assert
        assertThat(folderId.toString()).isEqualTo("d3adc0d3-0000-8300-8000-000000003039")
    }

    @Test
    fun `given FolderId created by of when toLegacyId is called then returns the original legacy id`() {
        // Arrange
        val legacyIds = listOf(0L, 1L, 42L, 65_536L, 4_294_967_296L, 281_474_976_710_656L, Long.MAX_VALUE)

        legacyIds.forEach { legacyId ->
            // Act
            val result = LegacyFolderIdFactory.toLegacyId(LegacyFolderIdFactory.of(legacyId))

            // Assert
            assertThat(result).isEqualTo(legacyId)
        }
    }

    @Test
    fun `given FolderId created by of when isLegacy is called then returns true`() {
        // Arrange
        val folderId = LegacyFolderIdFactory.of(legacyId = 1L)

        // Act
        val result = LegacyFolderIdFactory.isLegacy(folderId)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `given FolderId with random UUID when isLegacy is called then returns false`() {
        // Arrange
        val folderId = FolderId(Uuid.random())

        // Act
        val result = LegacyFolderIdFactory.isLegacy(folderId)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `given negative legacy id when of is called then throws IllegalArgumentException`() {
        // Arrange
        val legacyId = -1L

        // Act & Assert
        assertFailure { LegacyFolderIdFactory.of(legacyId) }.isInstanceOf<IllegalArgumentException>()
    }
}
