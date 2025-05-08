package com.fsck.k9.message.quote

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.TestCoreResourceProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.testing.crlf
import java.util.Date
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class TextQuoteCreatorTest : RobolectricTest() {
    val sentDate = Date(1540421219L)
    val originalMessage = mock<Message> {
        on { sentDate } doReturn sentDate
        on { from } doReturn Address.parse("Alice <alice@sender.example>")
        on { getRecipients(RecipientType.TO) } doReturn Address.parse("bob@recipient.example")
        on { getRecipients(RecipientType.CC) } doReturn emptyArray<Address>()
        on { subject } doReturn "Message subject"
    }
    val quoteDateFormatter = mock<QuoteDateFormatter> {
        on { format(eq(sentDate)) } doReturn "January 18, 1970 7:53:41 PM UTC"
    }
    val textQuoteCreator = TextQuoteCreator(quoteDateFormatter, TestCoreResourceProvider())

    @Test
    fun prefixQuote() {
        val messageBody = "Line 1\r\nLine 2\r\nLine 3"
        val quoteStyle = QuoteStyle.PREFIX
        val quotePrefix = "> "

        val quote = createQuote(messageBody, quoteStyle, quotePrefix)

        assertThat(quote).isEqualTo(
            """
            On January 18, 1970 7:53:41 PM UTC, Alice <alice@sender.example> wrote:
            > Line 1
            > Line 2
            > Line 3
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun prefixQuote_withPrefixThatNeedsEncoding() {
        val messageBody = "Line 1\r\nLine 2"
        val quoteStyle = QuoteStyle.PREFIX
        val quotePrefix = "$1\\t "

        val quote = createQuote(messageBody, quoteStyle, quotePrefix)

        assertThat(quote).isEqualTo(
            """
            On January 18, 1970 7:53:41 PM UTC, Alice <alice@sender.example> wrote:
            $1\t Line 1
            $1\t Line 2
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun prefixQuote_withLongLines() {
        val messageBody =
            """
            [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------]
            [-------------------------------------------------------------------------------------------------]
            """.trimIndent().crlf()
        val quoteStyle = QuoteStyle.PREFIX
        val quotePrefix = "> "

        val quote = createQuote(messageBody, quoteStyle, quotePrefix)

        assertThat(quote).isEqualTo(
            """
            On January 18, 1970 7:53:41 PM UTC, Alice <alice@sender.example> wrote:
            > [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------] [-------]
            > [-------------------------------------------------------------------------------------------------]
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun headerQuote() {
        val messageBody = "Line 1\r\nLine 2\r\nLine 3"
        val quoteStyle = QuoteStyle.HEADER

        val quote = createQuote(messageBody, quoteStyle)

        assertThat(quote).isEqualTo(
            """

            -------- Original Message --------
            From: Alice <alice@sender.example>
            Sent: January 18, 1970 7:53:41 PM UTC
            To: bob@recipient.example
            Subject: Message subject

            Line 1
            Line 2
            Line 3
            """.trimIndent().crlf(),
        )
    }

    private fun createQuote(messageBody: String, quoteStyle: QuoteStyle, quotePrefix: String = ""): String {
        return textQuoteCreator.quoteOriginalTextMessage(originalMessage, messageBody, quoteStyle, quotePrefix)
    }
}
