package net.thunderbird.feature.account.avatar

import java.text.BreakIterator

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
        return name.replace(" ", "").takeGraphemeClusters(MONOGRAM_LENGTH).uppercase()
    }

    /**
     * Returns the first [count] grapheme clusters of this string, or the whole string if it contains
     * fewer than [count] clusters.
     */
    private fun String.takeGraphemeClusters(count: Int): String {
        if (isEmpty() || count <= 0) {
            return ""
        }
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        var boundary = iterator.first()
        repeat(count) {
            val next = iterator.next()
            boundary = if (next == BreakIterator.DONE) length else next
        }
        return substring(0, boundary)
    }

    private companion object {
        private const val AVATAR_MONOGRAM_DEFAULT = "XX"
        private const val MONOGRAM_LENGTH = 2
    }
}
