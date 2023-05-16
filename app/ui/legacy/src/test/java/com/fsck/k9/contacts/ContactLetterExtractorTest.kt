package com.fsck.k9.contacts

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mail.Address
import org.junit.Test

class ContactLetterExtractorTest : RobolectricTest() {
    val letterExtractor = ContactLetterExtractor()

    @Test
    fun extractContactLetter_withNoNameUsesAddress() {
        assertExtractedLetterEquals("C", "<c@d.com>")
    }

    @Test
    fun extractContactLetter_withAsciiName() {
        assertExtractedLetterEquals("A", "abcd <a@b.com>")
    }

    @Test
    fun extractContactLetter_withLstroke() {
        assertExtractedLetterEquals("Ł", "Łatynka <a@b.com>")
    }

    @Test
    fun extractContactLetter_withChinese() {
        assertExtractedLetterEquals("千", "千里之行﹐始于足下 <a@b.com>")
    }

    @Test
    fun extractContactLetter_withCombinedGlyphs() {
        assertExtractedLetterEquals("\u0041\u0300", "\u0061\u0300 <a@b.com>")
    }

    @Test
    fun extractContactLetter_withSurrogatePair() {
        assertExtractedLetterEquals("\uD800\uDFB5", "\uD800\uDFB5 <a@b.com>")
    }

    @Test
    fun extractContactLetter_ignoresSpace() {
        assertExtractedLetterEquals("A", " abcd <a@b.com>")
    }

    @Test
    fun extractContactLetter_ignoresUsePunctuation() {
        assertExtractedLetterEquals("A", "-a <a@b.com>")
    }

    @Test
    fun extractContactLetter_ignoresMatchEmoji() {
        assertExtractedLetterEquals("?", "\uD83D\uDE00 <a@b.com>")
    }

    private fun assertExtractedLetterEquals(expected: String, address: String) {
        assertThat(letterExtractor.extractContactLetter(Address(address))).isEqualTo(expected)
    }
}
