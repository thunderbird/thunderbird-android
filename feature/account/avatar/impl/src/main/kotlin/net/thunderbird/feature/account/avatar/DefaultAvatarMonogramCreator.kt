package net.thunderbird.feature.account.avatar

import java.text.BreakIterator

/**
 * Creates a monogram based on a name or email address.
 *
 * This implementation generates a monogram using grapheme cluster segmentation:
 * - If the input starts with an emoji (including ZWJ sequences, skin-tone modifiers, or flags),
 *   the full emoji grapheme is returned.
 * - Otherwise, the first two characters/graphemes are returned and converted to uppercase.
 */
class DefaultAvatarMonogramCreator : AvatarMonogramCreator {
    override fun create(name: String?, email: String?): String {
        return if (!name.isNullOrEmpty()) {
            composeAvatarMonogram(name)
        } else if (!email.isNullOrEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(input: String): String {
        val trimmed = input.replace(" ", "")
        if (trimmed.isEmpty()) return AVATAR_MONOGRAM_DEFAULT

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(trimmed)

        val firstEnd = iterator.next()
        if (firstEnd == BreakIterator.DONE) return AVATAR_MONOGRAM_DEFAULT

        val firstGrapheme = trimmed.substring(0, firstEnd)
        if (firstGrapheme.isEmoji()) {
            return firstGrapheme
        }

        val secondEnd = iterator.next()
        val monogram = if (secondEnd != BreakIterator.DONE) {
            trimmed.substring(0, secondEnd)
        } else {
            firstGrapheme
        }

        return monogram.uppercase()
    }

    private fun String.isEmoji(): Boolean {
        var i = 0
        while (i < length) {
            val codePoint = codePointAt(i)
            if (isEmojiCodePoint(codePoint)) return true
            i += Character.charCount(codePoint)
        }
        return false
    }

    private fun isEmojiCodePoint(codePoint: Int): Boolean {
        return codePoint in 0x1F300..0x1FAFF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0x1F1E6..0x1F1FF ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint == 0x200D
    }

    private companion object {
        private const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
