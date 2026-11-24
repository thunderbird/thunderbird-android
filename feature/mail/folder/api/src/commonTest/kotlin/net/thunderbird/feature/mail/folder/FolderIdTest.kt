package net.thunderbird.feature.mail.folder

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class FolderIdTest {

    private val rawUuid = "123e4567-e89b-42d3-a456-426655440000"
    private val json = Json

    @Test
    fun `should serialize FolderId to JSON string`() {
        val folderId = FolderIdFactory.of(rawUuid)

        val serialized = json.encodeToString(FolderId.serializer(), folderId)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize FolderId from JSON string`() {
        val expectedFolderId = FolderIdFactory.of(rawUuid)

        val deserialized = json.decodeFromString(FolderId.serializer(), "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedFolderId)
    }
}
