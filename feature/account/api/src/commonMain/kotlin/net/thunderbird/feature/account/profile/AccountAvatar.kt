package net.thunderbird.feature.account.profile

/**
 * Sealed interface representing the avatar of an account.
 */
sealed interface AccountAvatar {
    data class Monogram(
        val value: String,
    ) : AccountAvatar

    data class Image(
        val uri: String,
    ) : AccountAvatar

    data class Icon(
        val name: String,
    ) : AccountAvatar
}
