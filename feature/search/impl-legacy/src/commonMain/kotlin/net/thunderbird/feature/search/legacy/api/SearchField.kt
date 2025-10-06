package net.thunderbird.feature.search.legacy.api

/**
 * Represents a field that can be searched.
 */
interface SearchField {
    /**
     * The name of the field.
     */
    val fieldName: String

    /**
     * The type of the field.
     */
    val fieldType: SearchFieldType

    /**
     * An optional custom query template for this field.
     * This can be used to define how the field should be queried in a custom way.
     *
     * Only applicable for fields with [SearchFieldType.CUSTOM].
     */
    val customQueryTemplate: String?
}
