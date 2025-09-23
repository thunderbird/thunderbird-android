package net.thunderbird.feature.account

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.Id

/**
 * Constant for the unified account ID.
 *
 * This ID is used to identify the unified account, which is a special account for aggregation purposes.
 *
 * The unified account ID is represented by a nil UUID (all zeros).
 *
 * See [RFC 4122 Section 4.1.7](https://datatracker.ietf.org/doc/html/rfc4122#section-4.1.7) for more details on nil UUIDs.
 */
@OptIn(ExperimentalUuidApi::class)
val UnifiedAccountId: AccountId = Id(Uuid.NIL)

/**
 * Extension property to check if an [AccountId] is the unified account ID.
 */
val AccountId.isUnified: Boolean
    get() = this == UnifiedAccountId

/**
 * Ensures that the [AccountId] is not the unified account ID.
 */
fun AccountId.requireReal(): AccountId {
    check(!isUnified) { "Operation not allowed on unified account" }
    return this
}
