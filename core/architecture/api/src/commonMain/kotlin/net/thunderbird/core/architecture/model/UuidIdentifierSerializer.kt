package net.thunderbird.core.architecture.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class UuidIdentifierSerializer<T : BaseUuidIdentifier>(
    private val factory: (String) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UuidIdentifier", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): T {
        return factory(decoder.decodeString())
    }
}
