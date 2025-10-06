package net.thunderbird.feature.search.legacy.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val searchFieldSerializerModule = SerializersModule {
    polymorphic(SearchField::class) {
        // Register MessageSearchField as a subclass
        subclass(MessageSearchField::class, PolymorphicSearchFieldSerializer(MessageSearchField.serializer()))
    }
}

class PolymorphicSearchFieldSerializer<T : SearchField>(
    private val searchFieldSerializer: KSerializer<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        serialName = searchFieldSerializer.descriptor.serialName,
    ) {
        element("value", searchFieldSerializer.descriptor)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T =
        decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            decodeSerializableElement(descriptor, 0, searchFieldSerializer)
        }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) =
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, searchFieldSerializer, value)
        }
}
