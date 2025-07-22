package net.thunderbird.feature.search.legacy.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchField

/**
 * A serializer for [SearchField] that specifically handles [MessageSearchField].
 * This serializer is used to convert [MessageSearchField] to and from its string representation.
 */
object MessageSearchFieldAsSearchFieldSerializer : KSerializer<SearchField> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SearchField", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SearchField) {
        val enumValue = value as? MessageSearchField
            ?: throw IllegalArgumentException("Only MessageSearchField is supported")
        encoder.encodeString(enumValue.name)
    }

    override fun deserialize(decoder: Decoder): SearchField {
        val name = decoder.decodeString()
        return MessageSearchField.valueOf(name)
    }
}
