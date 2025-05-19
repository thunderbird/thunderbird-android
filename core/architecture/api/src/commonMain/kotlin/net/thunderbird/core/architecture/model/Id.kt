package net.thunderbird.core.architecture.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a unique identifier for an entity.
 *
 * @param T The type of the entity.
 *
 * @property value The underlying UUID value.
 */
@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class Id<T>(val value: Uuid) {

    /**
     * Returns the raw string representation of the ID.
     */
    fun asRaw(): String {
        return value.toString()
    }
}
