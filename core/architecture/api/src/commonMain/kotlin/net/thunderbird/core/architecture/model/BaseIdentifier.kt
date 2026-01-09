package net.thunderbird.core.architecture.model

import kotlin.uuid.ExperimentalUuidApi

/**
 * Represents a unique identifier for an entity.
 *
 * @param T The type of the underlying value (e.g., [kotlin.uuid.Uuid]).
 *
 * @property value The underlying value.
 */
@OptIn(ExperimentalUuidApi::class)
abstract class BaseIdentifier<T : Comparable<T>>(val value: T) : Comparable<BaseIdentifier<T>> {

    override fun compareTo(other: BaseIdentifier<T>): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseIdentifier<T>) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
