package net.thunderbird.core.featureflag.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the configuration of a feature flag in the registry.
 *
 * @property key Unique identifier for the feature flag.
 * @property default Default enabled/disabled state of the flag.
 * @property description Optional human-readable description of the flag's purpose.
 * @property type Data type of the flag attribute, defaults to Boolean.
 * @property timeToPromote Optional timeline for promoting this flag (e.g., from experimental to stable).
 */
@Serializable
data class FlagRegistry(
    val key: String,
    val default: Boolean,
    val description: String? = null,
    val type: FlagAttributeType = FlagAttributeType.Boolean,
    @SerialName("time_to_promote")
    val timeToPromote: String? = null,
)
