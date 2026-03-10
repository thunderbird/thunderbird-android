package net.thunderbird.core.architecture.model

/**
 * A generic interface for entities that can be identified by a unique identifier.
 *
 * @param T The type of the unique identifier, must be a subtype of [BaseIdentifier].
 */
interface Identifiable<T : BaseIdentifier<*>> {
    val id: T
}
