package net.thunderbird.core.architecture.model

/**
 * Interface representing an entity with a unique identifier.
 */
interface Identifiable<T> {
    val id: Id<T>
}
