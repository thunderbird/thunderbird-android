package com.fsck.k9.helper

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class SuspiciousAddressDetectorTest {

    @Test
    fun `plain latin text is not suspicious`() {
        assertThat(SuspiciousAddressDetector.isSuspicious("Jane Doe", "jane@example.com")).isFalse()
    }

    @Test
    fun `text written entirely in one non-latin script is not suspicious`() {
        assertThat(SuspiciousAddressDetector.isSuspicious("Иван Петров", "ivan@example.com")).isFalse()
    }

    @Test
    fun `mixing latin and cyrillic look-alike letters is suspicious`() {
        // "PayPal" with a Cyrillic 'а' (U+0430) substituted for the Latin 'a'.
        val spoofedName = "PаyPal Support"
        assertThat(SuspiciousAddressDetector.isSuspicious(spoofedName, "support@paypal.com")).isTrue()
    }

    @Test
    fun `mixing latin and greek look-alike letters is suspicious`() {
        // "Apple" with a Greek 'Α' (U+0391) substituted for the Latin 'A'.
        val spoofedName = "Αpple Support"
        assertThat(SuspiciousAddressDetector.isSuspicious(spoofedName, "support@apple.com")).isTrue()
    }

    @Test
    fun `right-to-left override character is suspicious`() {
        val spoofedName = "invoice‮gnp.exe"
        assertThat(SuspiciousAddressDetector.isSuspicious(spoofedName, "billing@example.com")).isTrue()
    }

    @Test
    fun `zero width characters are suspicious`() {
        val spoofedEmail = "sup​port@example.com"
        assertThat(SuspiciousAddressDetector.isSuspicious("Support", spoofedEmail)).isTrue()
    }

    @Test
    fun `unicode tag characters used for ASCII smuggling are suspicious`() {
        // U+E0041 is the tag form of Latin capital "A", from the deprecated Tags block.
        val smuggledText = "Support󠁁"
        assertThat(SuspiciousAddressDetector.isSuspicious(smuggledText, "support@example.com")).isTrue()
    }

    @Test
    fun `null personal name with a clean email is not suspicious`() {
        assertThat(SuspiciousAddressDetector.isSuspicious(null, "jane@example.com")).isFalse()
    }

    @Test
    fun `containsMixedScriptHomoglyphs is false for digits and punctuation only`() {
        assertThat(SuspiciousAddressDetector.containsMixedScriptHomoglyphs("12345 - Invoice #42")).isFalse()
    }
}
