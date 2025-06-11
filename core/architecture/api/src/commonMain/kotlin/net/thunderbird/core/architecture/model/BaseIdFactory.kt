package net.thunderbird.core.architecture.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abstract base for ID factories.
 *
 * This class provides a default implementation for creating and generating IDs.
 * It uses UUIDs as the underlying representation of the ID.
 *
 * Example usage:
 *
 * ```kotlin
 * class AccountIdFactory : BaseIdFactory<AccountId>()
 * ```
 *
 * @param T The type of the ID.
 */
@OptIn(ExperimentalUuidApi::class)
abstract class BaseIdFactory<T> : IdFactory<T> {
    override fun of(raw: String): Id<T> = Id(Uuid.parse(raw))

    override fun create(): Id<T> = Id(Uuid.random())
}
