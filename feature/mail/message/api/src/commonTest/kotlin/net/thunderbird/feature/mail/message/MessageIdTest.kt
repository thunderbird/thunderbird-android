package net.thunderbird.feature.mail.message

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class MessageIdTest {

    private val rawUuid = "123e4567-e89b-42d3-a456-426655440000"
    private val json = Json

    @Test
    fun `should serialize MessageId to JSON string`() {
        val messageId = MessageIdFactory.of(rawUuid)

        val serialized = json.encodeToString(MessageId.serializer(), messageId)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize MessageId from JSON string`() {
        val expectedMessageId = MessageIdFactory.of(rawUuid)

        val deserialized = json.decodeFromString(MessageId.serializer(), "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedMessageId)
    }
}
