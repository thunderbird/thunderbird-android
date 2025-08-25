package net.thunderbird.feature.search.api

/**
 * Represents the type of a search field.
 *
 * This enum defines the different types of fields that can be used in a search operation.
 * Each type corresponds to a specific kind of data that the field can hold.
 */
enum class SearchFieldType {
    /**
     * Represents a field that contains text.
     */
    TEXT,

    /**
     * Represents a field that contains numeric values.
     */
    NUMBER,

    /**
     * Represents a field that contains custom search capabilities.
     */
    CUSTOM,
}
