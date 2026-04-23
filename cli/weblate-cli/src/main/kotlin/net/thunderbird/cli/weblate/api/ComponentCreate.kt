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
 * Weblate Component Creation Payload
 *
 * @property name The name of the component
 * @property slug The slug identifier of the component
 * @property project The project url of the component
 * @property fileMask The file mask for translations
 * @property template The template for translations
 * @property fileFormat The file format (e.g., android: "aresource", compose-resource: "cmp-resource")
 * @property category The category url of the component
 * @property linkedComponent The url of the linked component to use as a base
 * @property repo The repository URL of the component
 * @property vcs The VCS type (e.g., git, github)
 * @property mergeStyle The merge style
 * @property config The configuration of the component
 */
@Serializable(with = ComponentCreate.ComponentCreateSerializer::class)
data class ComponentCreate(
    val name: String,
    val slug: String,
    val project: String,
    @SerialName("filemask")
    val fileMask: String,
    val template: String,
    @SerialName("file_format")
    val fileFormat: String,
    val category: String? = null,
    @SerialName("linked_component")
    val linkedComponent: String? = null,
    val repo: String,
    val vcs: String,
    @SerialName("merge_style")
    val mergeStyle: String,
    val config: ComponentConfig,
) {
    companion object ComponentCreateSerializer : KSerializer<ComponentCreate> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ComponentCreate") {
            element<String>("name")
            element<String>("slug")
            element<String>("project")
            element<String>("filemask")
            element<String>("template")
            element<String>("file_format")
            element<String?>("category", isOptional = true)
            element<String?>("linked_component", isOptional = true)
            element<String>("repo")
            element<String>("vcs")
            element<String>("merge_style")
            element("config", ComponentConfig.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): ComponentCreate {
            error("Deserialization is not supported for ComponentCreate")
        }

        override fun serialize(encoder: Encoder, value: ComponentCreate) {
            require(encoder is JsonEncoder) {
                "Expected JsonEncoder, got ${encoder::class.simpleName}"
            }

            val json = buildJsonObject {
                put("name", value.name)
                put("slug", value.slug)
                put("project", value.project)
                put("filemask", value.fileMask)
                put("template", value.template)
                put("file_format", value.fileFormat)
                value.category?.let { put("category", it) }
                value.linkedComponent?.let { put("linked_component", it) }
                put("repo", value.repo)
                put("vcs", value.vcs)
                put("merge_style", value.mergeStyle)

                val configJson = encoder.json.encodeToJsonElement(ComponentConfig.serializer(), value.config)
                configJson.jsonObject.forEach { (key, value) ->
                    put(key, value)
                }
            }

            encoder.encodeJsonElement(json)
        }
    }
}
