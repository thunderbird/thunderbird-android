package net.thunderbird.feature.mail.message

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class ThreadIdTest {

    private val rawUuid = "123e4567-e89b-42d3-a456-426655440000"
    private val json = Json

    @Test
    fun `should serialize ThreadId to JSON string`() {
        val threadId = ThreadIdFactory.of(rawUuid)

        val serialized = json.encodeToString(ThreadId.serializer(), threadId)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize ThreadId from JSON string`() {
        val expectedThreadId = ThreadIdFactory.of(rawUuid)

        val deserialized = json.decodeFromString(ThreadId.serializer(), "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedThreadId)
    }
}
