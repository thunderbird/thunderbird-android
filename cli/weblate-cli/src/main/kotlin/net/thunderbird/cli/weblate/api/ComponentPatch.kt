package net.thunderbird.cli.weblate.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Weblate Component Patch
 *
 * We need the category to prevent the API from resetting it to undefined when we update the config.
 *
 * @property category The category url of the component
 * @property linkedComponent The url of the linked component
 * @property config The configuration of the component to be updated
 */
@Serializable(with = ComponentPatch.ComponentPatchSerializer::class)
data class ComponentPatch(
    val category: String?,
    @SerialName("linked_component")
    val linkedComponent: String?,
    val config: ComponentConfig,
) {
    companion object ComponentPatchSerializer : KSerializer<ComponentPatch> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ComponentPatch") {
            element<String>("category")
            element<String>("linked_component")
            element("config", ComponentConfig.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): ComponentPatch {
            error("Deserialization is not supported for ComponentPatch")
        }

        override fun serialize(encoder: Encoder, value: ComponentPatch) {
            require(encoder is JsonEncoder) {
                "Expected JsonEncoder, got ${encoder::class.simpleName}"
            }

            val config = encoder.json.encodeToJsonElement(ComponentConfig.serializer(), value.config)

            val json = buildJsonObject {
                value.category?.let { put("category", it) }
                value.linkedComponent?.let { put("linked_component", it) }
                config.jsonObject.forEach { (key, value) ->
                    put(key, value)
                }
            }

            encoder.encodeJsonElement(json)
        }
    }
}
