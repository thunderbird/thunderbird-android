package com.fsck.k9.helper

/**
 * Flags a sender's display name or email address as potentially spoofed.
 *
 * This targets three real, well-known ways a "From" address can be made to visually deceive a
 * reader without actually being the address it appears to be:
 *
 * - Bidirectional control characters (e.g. U+202E RIGHT-TO-LEFT OVERRIDE) can reorder how
 *   surrounding characters are displayed, making a malicious address visually read as a
 *   different, trusted one.
 * - Zero-width characters and the Unicode Tag block (U+E0000-U+E007F) can hide additional
 *   content inside what looks like a short, innocuous string ("ASCII smuggling").
 * - Mixing Latin letters with visually-confusable letters from another script (e.g. Cyrillic
 *   "а" U+0430 next to Latin "a" U+0061) is a classic homoglyph attack against a reader who
 *   only expects Latin text. Text written entirely in one non-Latin script is not flagged;
 *   only the mixture with Latin is treated as suspicious.
 *
 * This is a heuristic, not a full Unicode confusables-table lookup: it catches common attack
 * shapes so the UI can surface a warning. A flagged address is not proven malicious, and an
 * unflagged one is not proven safe.
 */
object SuspiciousAddressDetector {

    private val BIDI_CONTROL_CHARACTERS = setOf(
        '‪', // LEFT-TO-RIGHT EMBEDDING
        '‫', // RIGHT-TO-LEFT EMBEDDING
        '‬', // POP DIRECTIONAL FORMATTING
        '‭', // LEFT-TO-RIGHT OVERRIDE
        '‮', // RIGHT-TO-LEFT OVERRIDE
        '⁦', // LEFT-TO-RIGHT ISOLATE
        '⁧', // RIGHT-TO-LEFT ISOLATE
        '⁨', // FIRST STRONG ISOLATE
        '⁩', // POP DIRECTIONAL ISOLATE
    )

    private val INVISIBLE_CHARACTERS = setOf(
        '​', // ZERO WIDTH SPACE
        '‌', // ZERO WIDTH NON-JOINER
        '‍', // ZERO WIDTH JOINER
        '\uFEFF', // ZERO WIDTH NO-BREAK SPACE / BOM
    )

    private const val TAG_BLOCK_START = 0xE0000
    private const val TAG_BLOCK_END = 0xE007F

    private val LATIN_LOOKING_BLOCKS = setOf(
        Character.UnicodeBlock.BASIC_LATIN,
        Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
        Character.UnicodeBlock.LATIN_EXTENDED_A,
        Character.UnicodeBlock.LATIN_EXTENDED_B,
    )

    private val CONFUSABLE_BLOCKS = setOf(
        Character.UnicodeBlock.CYRILLIC,
        Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY,
        Character.UnicodeBlock.GREEK,
    )

    /**
     * True if [text] contains a bidirectional override, embedding, or isolate control
     * character.
     */
    fun containsBidiControlCharacters(text: String): Boolean {
        return text.any { it in BIDI_CONTROL_CHARACTERS }
    }

    /**
     * True if [text] contains a zero-width character or a Unicode Tag character, either of
     * which can hide content from a casual reading of the visible text.
     */
    fun containsInvisibleCharacters(text: String): Boolean {
        if (text.any { it in INVISIBLE_CHARACTERS }) {
            return true
        }
        return containsTagBlockCharacter(text)
    }

    private fun containsTagBlockCharacter(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (codePoint in TAG_BLOCK_START..TAG_BLOCK_END) {
                return true
            }
            index += Character.charCount(codePoint)
        }
        return false
    }

    /**
     * True if [text] mixes Latin letters with letters from a script commonly used for
     * homoglyph attacks against Latin-reading users (e.g. Cyrillic, Greek).
     */
    fun containsMixedScriptHomoglyphs(text: String): Boolean {
        var hasLatinLetter = false
        var hasConfusableLetter = false

        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (Character.isLetter(codePoint)) {
                val block = Character.UnicodeBlock.of(codePoint)
                if (block in LATIN_LOOKING_BLOCKS) {
                    hasLatinLetter = true
                } else if (block in CONFUSABLE_BLOCKS) {
                    hasConfusableLetter = true
                }
                if (hasLatinLetter && hasConfusableLetter) {
                    return true
                }
            }
            index += Character.charCount(codePoint)
        }
        return false
    }

    /**
     * True if either [personal] (the decoded display name) or [emailAddress] shows a sign of
     * spoofing: bidirectional control characters, hidden characters, or mixed-script
     * homoglyphs.
     */
    fun isSuspicious(personal: String?, emailAddress: String): Boolean {
        return listOfNotNull(personal, emailAddress).any { text ->
            containsBidiControlCharacters(text) ||
                containsInvisibleCharacters(text) ||
                containsMixedScriptHomoglyphs(text)
        }
    }
}
