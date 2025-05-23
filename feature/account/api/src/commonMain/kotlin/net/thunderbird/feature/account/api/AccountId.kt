package net.thunderbird.feature.account.api

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class AccountId private constructor(
    val value: String,
) {
    companion object {

        /**
         * Create an [AccountId] from a [String].
         */
        @OptIn(ExperimentalUuidApi::class)
        fun from(id: String): AccountId {
            try {
                return AccountId(Uuid.parse(id).toString())
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid AccountId: $id", exception)
            }
        }

        @OptIn(ExperimentalUuidApi::class)
        fun create(): AccountId {
            return AccountId(Uuid.random().toString())
        }
    }
}
