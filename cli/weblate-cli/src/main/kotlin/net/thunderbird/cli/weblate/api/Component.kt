package net.thunderbird.cli.weblate.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = Component.ComponentSerializer::class)
data class Component(
    val info: ComponentInfo,
    val config: ComponentConfig,
) {
    companion object ComponentSerializer : KSerializer<Component> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Component") {
            element("info", ComponentInfo.serializer().descriptor)
            element("config", ComponentConfig.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): Component {
            require(decoder is JsonDecoder) {
                "Expected JsonDecoder, got ${decoder::class.simpleName}"
            }

            val jsonObject = decoder.decodeJsonElement().jsonObject

            val info = decoder.json.decodeFromJsonElement(ComponentInfo.serializer(), jsonObject)
            val config = decoder.json.decodeFromJsonElement(ComponentConfig.serializer(), jsonObject)

            return Component(
                info = info,
                config = config,
            )
        }

        override fun serialize(
            encoder: Encoder,
            value: Component,
        ) {
            error("Component serialization is not supported")
        }
    }
}
