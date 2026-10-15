package net.thunderbird.core.architecture.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalUuidApi::class)
class UuidIdentifierSerializerTest {

    @Serializable(with = TestIdSerializer::class)
    private class TestId(value: Uuid) : BaseUuidIdentifier(value)

    private object TestIdFactory {
        fun of(raw: String): TestId = TestId(Uuid.parse(raw))
    }

    private object TestIdSerializer : KSerializer<TestId> by UuidIdentifierSerializer(TestIdFactory::of)

    private val rawUuid = "123e4567-e89b-12d3-a456-426655440000"
    private val json = Json

    @Test
    fun `should serialize BaseUuidIdentifier to JSON string`() {
        val id = TestId(Uuid.parse(rawUuid))

        val serialized = json.encodeToString(TestIdSerializer, id)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize BaseUuidIdentifier from JSON string`() {
        val expectedId = TestId(Uuid.parse(rawUuid))

        val deserialized = json.decodeFromString(TestIdSerializer, "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedId)
        assertThat(deserialized.value).isEqualTo(expectedId.value)
    }
}
