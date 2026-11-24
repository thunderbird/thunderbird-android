package net.thunderbird.feature.account.avatar

import net.thunderbird.feature.account.AccountId

/**
 * Sealed interface representing the avatar of an account.
 */
sealed interface Avatar {
    val accountId: AccountId

    data class Monogram(
        override val accountId: AccountId,
        val value: String,
    ) : Avatar

    data class Image(
        override val accountId: AccountId,
        val uri: String,
    ) : Avatar

    data class Icon(
        override val accountId: AccountId,
        val name: String,
    ) : Avatar
}
