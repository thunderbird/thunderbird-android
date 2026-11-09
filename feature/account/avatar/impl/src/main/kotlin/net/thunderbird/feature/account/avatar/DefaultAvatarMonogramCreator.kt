package net.thunderbird.feature.account.avatar

import net.thunderbird.core.common.text.GraphemeClusters

/**
 * Creates a monogram based on a name or email address.
 *
 * This implementation generates a monogram by taking the first two grapheme clusters of the name or
 * email, removing spaces, and converting them to uppercase. Grapheme clusters are used so that
 * multi-code-point characters such as emoji (including ZWJ sequences like 🐦‍🔥) are kept intact
 * instead of being split into their individual code points.
 */
class DefaultAvatarMonogramCreator : AvatarMonogramCreator {
    override fun create(name: String?, email: String?): String {
        return if (name != null && name.isNotEmpty()) {
            composeAvatarMonogram(name)
        } else if (email != null && email.isNotEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(name: String): String {
        return GraphemeClusters.take(name.replace(" ", ""), MONOGRAM_LENGTH).uppercase()
    }

    private companion object {
        private const val AVATAR_MONOGRAM_DEFAULT = "XX"
        private const val MONOGRAM_LENGTH = 2
    }
}
