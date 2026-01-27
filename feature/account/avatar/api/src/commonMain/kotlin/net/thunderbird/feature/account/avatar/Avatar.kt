package net.thunderbird.feature.account.avatar

/**
 * Sealed interface representing the avatar of an account.
 */
sealed interface Avatar {
    data class Monogram(
        val value: String,
    ) : Avatar

    data class Image(
        val uri: String,
    ) : Avatar

    data class Icon(
        val name: String,
    ) : Avatar
}
