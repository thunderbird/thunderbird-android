package net.thunderbird.feature.mail.message

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class AttachmentIdTest {

    private val rawUuid = "123e4567-e89b-42d3-a456-426655440000"
    private val json = Json

    @Test
    fun `should serialize AttachmentId to JSON string`() {
        val attachmentId = AttachmentIdFactory.of(rawUuid)

        val serialized = json.encodeToString(AttachmentId.serializer(), attachmentId)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize AttachmentId from JSON string`() {
        val expectedAttachmentId = AttachmentIdFactory.of(rawUuid)

        val deserialized = json.decodeFromString(AttachmentId.serializer(), "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedAttachmentId)
    }
}
