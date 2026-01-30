package net.thunderbird.core.architecture.model

/**
 * A generic factory for creating instances of a specific [BaseIdentifier] type.
 *
 * @param T The concrete [BaseIdentifier] type (e.g., AccountId).
 */
interface IdentifierFactory<T : BaseIdentifier<*>> {

    /**
     * Creates a [T] from its raw string representation.
     *
     * This method expects a valid string representation and is designed to "fail-fast". It will throw
     * an [IllegalArgumentException] if the raw string cannot be parsed into a valid ID.
     * This indicates a programming error or data corruption, which should be surfaced
     * immediately rather than handled as an expected nullable path.
     *
     * @param raw The raw string representation of the ID, which must be valid.
     * @return A non-null instance of [T] representing the ID.
     * @throws IllegalArgumentException if the raw string is malformed.
     */
    fun of(raw: String): T

    /**
     * Creates a new [T].
     *
     * @return A new instance of [T] representing the created ID.
     */
    fun create(): T
}
