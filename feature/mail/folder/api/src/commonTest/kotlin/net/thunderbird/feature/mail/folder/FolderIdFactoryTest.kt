package net.thunderbird.feature.mail.folder

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FolderIdFactoryTest {

    @Test
    fun `given existing UUID when of is called then returns compatible FolderId`() {
        // Arrange
        val raw = "123e4567-e89b-42d3-a456-426655440000"

        // Act
        val folderId = FolderIdFactory.of(raw)

        // Assert
        assertThat(folderId.value).isEqualTo(Uuid.parse(raw))
    }

    @Test
    fun `given create is called then returns UUIDv7 FolderId`() {
        // Arrange + Act
        val folderId = FolderIdFactory.create()

        // Assert
        folderId.value.toLongs { mostSignificantBits, leastSignificantBits ->
            assertThat((mostSignificantBits ushr 12) and 0x0F).isEqualTo(7L)
            assertThat(leastSignificantBits ushr 62).isEqualTo(2L)
        }
    }
}
