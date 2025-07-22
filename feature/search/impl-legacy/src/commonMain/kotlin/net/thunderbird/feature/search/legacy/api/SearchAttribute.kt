package net.thunderbird.feature.search.legacy.api

import kotlinx.serialization.Serializable

/**
 * Represents the attributes that can be used to match search conditions.
 */
@Serializable
enum class SearchAttribute {
    /**
     * The value must be contained within the field.
     */
    CONTAINS,

    /**
     * The value must be equal with the field.
     */
    EQUALS,

    /**
     * The value must not be equal with the field.
     */
    NOT_EQUALS,
}
