package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

private const val MESSAGE_ID = "<left-side@domain.example>"

class MessageIdParserTest {
    @Test
    fun `typical message identifiers`() {
        assertMessageIdValid("<left-side@right-side>")
        assertMessageIdValid("<left-side@domain.example>")
    }

    @Test
    fun `message identifier with domain literal`() {
        assertMessageIdValid("<left-side@[dtext]>")
    }

    @Test
    fun `message identifier with extra space`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, " ")
    }

    @Test
    fun `message identifier with multiple extra spaces`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, "   ")
    }

    @Test
    fun `message identifier with extra tab`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, "\t")
    }

    @Test
    fun `message identifier with extra comment`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, "(comment)")
    }

    @Test
    fun `message identifier with extra nested comments`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, "(comment one (nested comment (nested nested comment)))")
    }

    @Test
    fun `message identifier with extra comment and folding whitespace`() {
        assertMessageIdWithExtraValid(MESSAGE_ID, " \r\n\t(comment \\(\r\n more comment)\r\n \t")
    }

    @Test
    fun `message identifier with excessive extra comment nesting`() {
        val extra = "(".repeat(10_000) + ")".repeat(10_000)
        assertMessageIdWithExtraValid(MESSAGE_ID, extra)
    }

    @Test
    fun `multiple message identifiers`() {
        val messageId1 = "<left-side@right-side>"
        val messageId2 = "<left-side@domain.example>"

        assertMessageIdsValid("$messageId1 $messageId2", listOf(messageId1, messageId2))
    }

    @Test
    fun `multiple message identifiers without separation`() {
        val messageId1 = "<left-side@right-side>"
        val messageId2 = "<left-side@domain.example>"

        assertMessageIdsValid("$messageId1$messageId2", listOf(messageId1, messageId2))
    }

    @Test
    fun `multiple message identifiers separated by tab`() {
        val messageId1 = "<left-side@right-side>"
        val messageId2 = "<left-side@domain.example>"

        assertMessageIdsValid("$messageId1\t$messageId2", listOf(messageId1, messageId2))
    }

    @Test
    fun `multiple message identifiers separated by line break`() {
        val messageId1 = "<left-side@right-side>"
        val messageId2 = "<left-side@domain.example>"

        assertMessageIdsValid("$messageId1\r\n $messageId2", listOf(messageId1, messageId2))
    }

    @Test
    fun `multiple message identifiers separated by comment`() {
        val messageId1 = "<left-side@right-side>"
        val messageId2 = "<left-side@domain.example>"

        assertMessageIdsValid("$messageId1(comment <this.is@ignored>)$messageId2", listOf(messageId1, messageId2))
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifier with additional data should throw`() {
        MessageIdParser.parse("$MESSAGE_ID extra")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifiers with additional data should throw`() {
        MessageIdParser.parseList("<one@domain.example> <two@domain.example> extra")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifier missing angle brackets`() {
        MessageIdParser.parse("left-side@domain.example")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifier missing left side`() {
        MessageIdParser.parse("<@domain.example>")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifier containing only left side`() {
        MessageIdParser.parse("<left-side>")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `message identifier missing right side`() {
        MessageIdParser.parse("<left-side@>")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `empty input`() {
        MessageIdParser.parse("")
    }

    @Test(expected = MimeHeaderParserException::class)
    fun `empty input for list`() {
        MessageIdParser.parseList("")
    }

    private fun assertMessageIdValid(input: String, expected: String = input) {
        showMimeHeaderParserError(input) {
            assertThat(MessageIdParser.parse(input)).isEqualTo(expected)
        }

        assertMessageIdsValid(input, listOf(expected))
    }

    private fun assertMessageIdsValid(input: String, expected: List<String>) {
        showMimeHeaderParserError(input) {
            val messageIds = MessageIdParser.parseList(input)
            assertThat(messageIds).isEqualTo(expected)
        }
    }

    /**
     * Test input with [extra] prepended, appended, and both at the same time.
     */
    @Suppress("SameParameterValue")
    private fun assertMessageIdWithExtraValid(messageId: String, extra: String) {
        assertMessageIdValid("$extra$messageId", messageId)
        assertMessageIdValid("$messageId$extra", messageId)
        assertMessageIdValid("$extra$messageId$extra", messageId)
    }

    private fun showMimeHeaderParserError(input: String, block: () -> Unit) {
        try {
            block()
        } catch (e: MimeHeaderParserException) {
            // Replace tabs with spaces so the error indicator lines up
            val tweakedInput = input.replace("\t", " ")
            println("Input: $tweakedInput")
            println("Error: " + "-".repeat(e.errorIndex) + "^")
            throw e
        }
    }
}
