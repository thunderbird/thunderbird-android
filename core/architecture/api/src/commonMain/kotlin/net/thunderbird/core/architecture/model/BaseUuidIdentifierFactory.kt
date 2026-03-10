package net.thunderbird.core.architecture.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abstract base for UUID-based ID factories.
 *
 * This class provides a default implementation for creating and generating IDs.
 * It uses UUIDs as the underlying representation of the ID.
 *
 * Example usage:
 *
 * ```kotlin
 * class AccountIdFactory : BaseUuidIdentifierFactory<AccountId>(::AccountId)
 * ```
 *
 * @param T The concrete type of the ID.
 * @param fromUuid The function to convert a [Uuid] to the specific ID type [T].
 */
@OptIn(ExperimentalUuidApi::class)
abstract class BaseUuidIdentifierFactory<T : BaseUuidIdentifier>(
    private val fromUuid: (Uuid) -> T,
) : IdentifierFactory<T> {

    override fun of(raw: String): T = fromUuid(Uuid.parse(raw))

    override fun create(): T = fromUuid(Uuid.random())
}
