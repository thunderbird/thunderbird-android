package net.thunderbird.core.architecture.model

/**
 * Factory interface for creating and generating IDs.
 */
interface IdFactory<T> {

    /**
     * Creates an ID from a raw string representation.
     *
     * @param raw The raw string representation of the ID.
     * @return An instance of [Id] representing the ID.
     */
    fun create(raw: String): Id<T>

    /**
     * Generates a new ID.
     *
     * @return A new instance of [Id] representing the generated ID.
     */
    fun new(): Id<T>
}
