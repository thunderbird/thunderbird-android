package net.thunderbird.core.architecture.model

/**
 * Factory interface for creating and generating IDs.
 */
interface IdFactory<T> {

    /**
     * Creates an [Id] from a raw string representation.
     *
     * @param raw The raw string representation of the ID.
     * @return An instance of [Id] representing the ID.
     */
    fun of(raw: String): Id<T>

    /**
     * Creates a new [Id].
     *
     * @return A new instance of [Id] representing the created ID.
     */
    fun create(): Id<T>
}
