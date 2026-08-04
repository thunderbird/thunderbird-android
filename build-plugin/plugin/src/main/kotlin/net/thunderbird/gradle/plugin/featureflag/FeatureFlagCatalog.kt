package net.thunderbird.gradle.plugin.featureflag

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class FeatureFlagCatalog(
    val version: String,
    val flags: List<FlagRegistry>,
    val overrides: FlagRegistryOverrides,
)

@Serializable
data class FeatureFlagContext(
    @SerialName("targeting_key")
    val targetingKey: TargetingKey,
    val attributes: List<ContextAttribute>,
)

@Serializable
data class TargetingKey(
    val source: String,
    val description: String,
)

@Serializable
data class ContextAttribute(
    val name: String,
    val type: AttributeType,
    val source: String? = null,
    val derived: String? = null,
    val values: List<String> = emptyList(),
    val optional: Boolean = false,
)

@Serializable
enum class AttributeType {
    @SerialName("string")
    String,

    @SerialName("int")
    Int,

    @SerialName("double")
    Double,

    @SerialName("boolean")
    Boolean,
}

@Serializable
data class FlagRegistry(
    val key: String,
    val default: Boolean,
    val description: String? = null,
    val type: AttributeType = AttributeType.Boolean,
    @SerialName("target_feature")
    val targetFeature: String? = null,
    @SerialName("time_to_promote")
    val timeToPromote: String? = null,
)

@Serializable
data class FlagRegistryOverrides(
    val thunderbird: ThunderbirdOverrides,
    val k9: K9Overrides,
)

@Serializable
data class ThunderbirdOverrides(
    val debug: FlagOverrides,
    val daily: FlagOverrides,
    val beta: FlagOverrides,
    val release: FlagOverrides,
)

@Serializable
data class K9Overrides(
    val debug: FlagOverrides,
    val release: FlagOverrides,
)

typealias FlagOverrides = Map<String, Boolean>
