package com.fsck.k9.mail.internet

import org.junit.Assert.fail
import org.junit.Test

class MimeHeaderCheckerTest {
    @Test
    fun emptyValue() {
        assertValidHeader("Subject: ")
    }

    @Test
    fun blankValue() {
        assertValidHeader("Subject:  ")
    }

    @Test
    fun textEndingInSpace() {
        assertValidHeader("Subject: Text ")
    }

    @Test
    fun textContainingSpaces() {
        assertValidHeader("Subject: Text containing spaces")
    }

    @Test
    fun allVisibleCharacters() {
        val text = (33..126).map { it.toChar() }.joinToString("")
        assertValidHeader("Subject: $text")
    }

    @Test
    fun blankFirstLine() {
        assertValidHeader("Subject: \r\n Two")
    }

    @Test
    fun twoLines() {
        assertValidHeader("Subject: One\r\n Two")
    }

    @Test
    fun threeLines() {
        assertValidHeader("Subject: One\r\n Two\r\n Three")
    }

    @Test
    fun secondLineStartingWithTab() {
        assertValidHeader("Subject: One\r\n\tTwo")
    }

    @Test
    fun secondLineStartingWithMultipleWhitespace() {
        assertValidHeader("Subject: One\r\n \t Two")
    }

    @Test
    fun singleLineAtMaximumLineLength() {
        val longText = "x".repeat(998 /* text limit */ - 4 /* Test */ - 2 /* colon, space */)
        assertValidHeader("Test: $longText")
    }

    @Test
    fun firstLineAtMaximumLineLength() {
        val longText = "x".repeat(998 /* text limit */ - 4 /* Test */ - 2 /* colon, space */)
        assertValidHeader("Test: $longText\r\n Text")
    }

    @Test
    fun middleLineAtMaximumLineLength() {
        val longText = "x".repeat(998 - 1 /* space */)
        assertValidHeader("Test: One\r\n $longText\r\n Three")
    }

    @Test
    fun lastLineAtMaximumLineLength() {
        val longText = "x".repeat(998 - 1 /* space */)
        assertValidHeader("Test: One\r\n $longText")
    }

    @Test
    fun colonInHeaderName() {
        assertInvalidHeader("Header:Name: Text")
    }

    @Test
    fun nonAsciiCharacterInHeaderName() {
        assertInvalidHeader("Sübject: Text")
    }

    @Test
    fun headerNameExceedingLineLimit() {
        val longName = "x".repeat(998 - 2 /* space, colon */ + 1)
        assertInvalidHeader("$longName: ")
    }

    @Test
    fun nonAsciiCharacter() {
        assertInvalidHeader("Subject: ö")
    }

    @Test
    fun nonVisibleCharacter() {
        assertInvalidHeader("Subject: \u0007")
    }

    @Test
    fun endingInCR() {
        assertInvalidHeader("Subject: Text\r")
    }

    @Test
    fun endingInLF() {
        assertInvalidHeader("Subject: Text\n")
    }

    @Test
    fun endingInCRLF() {
        assertInvalidHeader("Subject: Text\r\n")
    }

    @Test
    fun lineBreakNotFollowedByWhitespace() {
        assertInvalidHeader("Subject: One\r\nTwo")
    }

    @Test
    fun singleCR() {
        assertInvalidHeader("Subject: One\rTwo")
    }

    @Test
    fun singleCrFollowedByWhitespace() {
        assertInvalidHeader("Subject: One\r Two")
    }

    @Test
    fun consecutiveCRs() {
        assertInvalidHeader("Subject: \r\r\n Two")
    }

    @Test
    fun singleLF() {
        assertInvalidHeader("Subject: One\nTwo")
    }

    @Test
    fun singleLfFollowedByWhitespace() {
        assertInvalidHeader("Subject: One\n Two")
    }

    @Test
    fun consecutiveLFs() {
        assertInvalidHeader("Subject: \r\n\n Two")
    }

    @Test
    fun consecutiveLineBreaks() {
        assertInvalidHeader("Subject: One\r\n\r\n Two")
    }

    @Test
    fun blankMiddleLine() {
        assertInvalidHeader("Subject: One\r\n \r\n Two")
    }

    @Test
    fun endsWithBlankLine() {
        assertInvalidHeader("Subject: One\r\n ")
    }

    @Test
    fun singleLineExceedingLineLength() {
        val longText = "x".repeat(998 /* text limit */ - 4 /* Test */ - 2 /* colon, space */ + 1)
        assertInvalidHeader("Test: $longText")
    }

    @Test
    fun firstLineExceedingLineLength() {
        val longText = "x".repeat(998 /* text limit */ - 4 /* Test */ - 2 /* colon, space */ + 1)
        assertInvalidHeader("Test: $longText\r\n Text")
    }

    @Test
    fun middleLineExceedingLineLength() {
        val longText = "x".repeat(998 - 1 /* space */ + 1)
        assertInvalidHeader("Test: One\r\n $longText\r\n Three")
    }

    @Test
    fun lastLineExceedingLineLength() {
        val longText = "x".repeat(998 - 1 /* space */ + 1)
        assertInvalidHeader("Test: One\r\n $longText")
    }

    private fun assertValidHeader(header: String) {
        val (name, value) = header.split(": ", limit = 2)
        MimeHeaderChecker.checkHeader(name, value)
    }

    private fun assertInvalidHeader(header: String) {
        val (name, value) = header.split(": ", limit = 2)
        try {
            MimeHeaderChecker.checkHeader(name, value)
            fail("Expected exception")
        } catch (expected: MimeHeaderParserException) {
        }
    }
}
