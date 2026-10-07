package net.thunderbird.feature.account.avatar

import net.thunderbird.feature.account.AccountId

/**
 * Sealed interface representing the avatar of an account.
 */
sealed interface Avatar {
    val id: AccountId

    data class Monogram(
        override val id: AccountId,
        val value: String,
    ) : Avatar

    data class Image(
        override val id: AccountId,
        val uri: String,
    ) : Avatar

    data class Icon(
        override val id: AccountId,
        val name: String,
    ) : Avatar
}
