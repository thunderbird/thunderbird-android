package net.thunderbird.core.featureflag.model

import kotlinx.serialization.SerialName

/**
 * Defines the supported data types for feature flag attributes.
 *
 * This enum specifies the type of value that a feature flag can hold, enabling
 * type-safe serialization and deserialization of flag configurations.
 */
enum class FlagAttributeType {
    @SerialName("string")
    String,

    @SerialName("int")
    Int,

    @SerialName("double")
    Double,

    @SerialName("boolean")
    Boolean,
}
