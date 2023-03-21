package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.crlf
import org.junit.Test

class FlowedMessageUtilsTest {
    @Test
    fun `deflow() with simple text`() {
        val input = "Text that should be \r\n" +
            "displayed on one line"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo("Text that should be displayed on one line")
    }

    @Test
    fun `deflow() with only some lines ending in space`() {
        val input = "Text that \r\n" +
            "should be \r\n" +
            "displayed on \r\n" +
            "one line.\r\n" +
            "Text that should retain\r\n" +
            "its line break."

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(
            """
            Text that should be displayed on one line.
            Text that should retain
            its line break.
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun `deflow() with nothing to do`() {
        val input = "Line one\r\nLine two\r\n"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `deflow() with quoted text`() {
        val input = "On [date], [user] wrote:\r\n" +
            "> Text that should be displayed \r\n" +
            "> on one line\r\n" +
            "\r\n" +
            "Some more text that should be \r\n" +
            "displayed on one line.\r\n"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(
            """
            |On [date], [user] wrote:
            |> Text that should be displayed on one line
            |
            |Some more text that should be displayed on one line.
            |
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun `deflow() with quoted text ending in space`() {
        val input = "> Quoted text \r\n" +
            "Some other text"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo("> Quoted text \r\nSome other text")
    }

    @Test
    fun `deflow() with quoted text ending in space before quoted text of different quoting depth`() {
        val input = ">> Depth 2 \r\n" +
            "> Depth 1 \r\n" +
            "> is here\r\n" +
            "Some other text"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(
            """
            >> Depth 2${" "}
            > Depth 1 is here
            Some other text
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun `deflow() with quoted text ending in space followed by empty line`() {
        val input = "> Quoted \r\n" +
            "\r\n" +
            "Text"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `deflow() with delSp=true`() {
        val input = "Text that is wrapped mid wo \r\nrd"

        val result = FlowedMessageUtils.deflow(input, delSp = true)

        assertThat(result).isEqualTo("Text that is wrapped mid word")
    }

    @Test
    fun `deflow() with quoted text and space-stuffing and delSp=true`() {
        val input = "> Quoted te \r\n" +
            "> xt"

        val result = FlowedMessageUtils.deflow(input, delSp = true)

        assertThat(result).isEqualTo("> Quoted text")
    }

    @Test
    fun `deflow() with space-stuffed second line`() {
        val input = "Text that should be \r\n" +
            " displayed on one line"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo("Text that should be displayed on one line")
    }

    @Test
    fun `deflow() with only space-stuffing`() {
        val input = "Line 1\r\n" +
            " Line 2\r\n" +
            " Line 3\r\n"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo("Line 1\r\nLine 2\r\nLine 3\r\n")
    }

    @Test
    fun `deflow() with quoted space-stuffed second line`() {
        val input = "> Text that should be \r\n" +
            "> displayed on one line"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo("> Text that should be displayed on one line")
    }

    @Test
    fun `deflow() with text containing signature`() {
        val input = "Text that should be \r\n" +
            "displayed on one line.\r\n" +
            "\r\n" +
            "-- \r\n" +
            "Signature \r\n" +
            "text"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(
            """
            Text that should be displayed on one line.
            
            --${" "}
            Signature text
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun `deflow() with quoted text containing signature`() {
        val input = "> Text that should be \r\n" +
            "> displayed on one line.\r\n" +
            "> \r\n" +
            "> -- \r\n" +
            "> Signature \r\n" +
            "> text"

        val result = FlowedMessageUtils.deflow(input, delSp = false)

        assertThat(result).isEqualTo(
            """
            > Text that should be displayed on one line.
            >${" "}
            > --${" "}
            > Signature text
            """.trimIndent().crlf(),
        )
    }

    @Test
    fun `deflow() with flowed line followed by signature separator`() {
        val input = "Fake flowed line \r\n" +
            "-- \r\n" +
            "Signature"

        val result = FlowedMessageUtils.deflow(input, delSp = true)

        assertThat(result).isEqualTo(
            """
            Fake flowed line
            --${" "}
            Signature
            """.trimIndent().crlf(),
        )
    }
}
