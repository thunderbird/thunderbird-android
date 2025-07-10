package net.thunderbird.feature.search.api

/**
 * Represents the attributes that can be used to match search conditions.
 */
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
