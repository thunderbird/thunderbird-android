package com.fsck.k9.message.html

import com.fsck.k9.mail.crlf
import com.fsck.k9.mail.removeLineBreaks
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlConverterTest {
    @Test
    fun `textToHtml() should convert quoted text using blockquote tags`() {
        val message =
            """
            Panama!
            
            Bob Barker <bob@aol.com> wrote:
            > a canal
            >
            > Dorothy Jo Gideon <dorothy@aol.com> espoused:
            > >A man, a plan...
            > Too easy!
            
            Nice job :)
            >> Guess!
            """.trimIndent().crlf()

        val result = HtmlConverter.textToHtml(message)

        assertThat(result).isEqualTo(
            """
            |<pre dir="auto" class="k9mail">
            |Panama!<br>
            |<br>
            |Bob Barker &lt;bob@aol.com&gt; wrote:<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;">
            | a canal<br>
            |<br>
            | Dorothy Jo Gideon &lt;dorothy@aol.com&gt; espoused:<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;">
            |A man, a plan...<br>
            |</blockquote>
            |Too easy!<br>
            |</blockquote>
            |<br>
            |Nice job :)<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;">
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;">
            |Guess!
            |</blockquote>
            |</blockquote>
            |</pre>
            """.trimMargin().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should retain indentation inside quoted text`() {
        val message =
            """
            *facepalm*
            
            Bob Barker <bob@aol.com> wrote:
            > A wise man once said...
            >
            >     LOL F1RST!!!!!
            >
            > :)
            """.trimIndent().crlf()

        val result = HtmlConverter.textToHtml(message)

        assertThat(result).isEqualTo(
            """
            |<pre dir="auto" class="k9mail">
            |*facepalm*<br>
            |<br>
            |Bob Barker &lt;bob@aol.com&gt; wrote:<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;">
            | A wise man once said...<br>
            |<br>
            |     LOL F1RST!!!!!<br>
            |<br>
            | :)
            |</blockquote>
            |</pre>
            """.trimMargin().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() with various quotation depths`() {
        val message =
            """
            zero
            > one
            >> two
            >>> three
            >>>> four
            >>>>> five
            >>>>>> six
            """.trimIndent().crlf()

        val result = HtmlConverter.textToHtml(message)

        assertThat(result).isEqualTo(
            """
            |<pre dir="auto" class="k9mail">
            |zero<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;">
            |one<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;">
            |two<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #8ae234; padding-left: 1ex;">
            |three<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #fcaf3e; padding-left: 1ex;">
            |four<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #e9b96e; padding-left: 1ex;">
            |five<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ccc; padding-left: 1ex;">
            |six
            |</blockquote>
            |</blockquote>
            |</blockquote>
            |</blockquote>
            |</blockquote>
            |</blockquote>
            |</pre>
            """.trimMargin().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should preserve spaces at the start of a line`() {
        val message =
            """
            |foo
            | bar
            |  baz
            |
            """.trimMargin().crlf()

        val result = HtmlConverter.textToHtml(message)

        assertThat(result).isEqualTo(
            """
            |<pre dir="auto" class="k9mail">
            |foo<br>
            | bar<br>
            |  baz<br>
            |</pre>
            """.trimMargin().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should preserve spaces at the start of a line followed by special characters`() {
        val message =
            """
            | 
            |  &
            |   ${" "}
            |   <
            |  >${" "}
            |
            """.trimMargin().crlf()

        val result = HtmlConverter.textToHtml(message)

        assertThat(result).isEqualTo(
            """
            |<pre dir="auto" class="k9mail">
            | <br>
            |  &amp;<br>
            |    <br>
            |   &lt;<br>
            |<blockquote class="gmail_quote" style="margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;">
            |<br>
            |</blockquote>
            |</pre>
            """.trimMargin().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should replace common horizontal divider ASCII patterns with HR tags`() {
        val text =
            """
            text
            ---------------------------
            some other text
            ===========================
            more text
            -=-=-=-=-=-=-=-=-=-=-=-=-=-
            scissors below
            -- >8 --
            other direction
            -- 8< --
            end
            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            text
            <hr>
            some other text
            <hr>
            more text
            <hr>
            scissors below
            <hr>
            other direction
            <hr>
            end
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should not convert dashes mixed with spaces to an HR tag`() {
        val text =
            """
            hello
            --- --- --- --- ---
            foo bar
            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            hello<br>
            --- --- --- --- ---<br>
            foo bar
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should merge consecutive horizontal dividers into a single HR tag`() {
        val text =
            """
            hello
            ------------
            ---------------
            foo bar
            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            hello
            <hr>
            foo bar
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should not replace dashed horizontal divider prefixed with text`() {
        val text = "hello----\n\n"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            hello----<br>
            <br>
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should not replace double dash with an HR tag`() {
        val text = "--\n"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo("""<pre dir="auto" class="k9mail">--<br></pre>""")
    }

    @Test
    fun `textToHtml() should not replace double equal sign with an HR tag`() {
        val text = "==\n"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo("""<pre dir="auto" class="k9mail">==<br></pre>""")
    }

    @Test
    fun `textToHtml() should not replace double underscore with an HR tag`() {
        val text = "__\n"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo("""<pre dir="auto" class="k9mail">__<br></pre>""")
    }

    @Test
    fun `textToHtml() should replace any combination of three consecutive divider characters with an HR tag`() {
        val text =
            """
            --=
            -=-
            ===
            ___

            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo("""<pre dir="auto" class="k9mail"><hr></pre>""")
    }

    @Test
    fun `textToHtml() should replace dashes at the start of the input`() {
        val text = "---------------------------\nfoo bar"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            <hr>
            foo bar
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should replace dashes at the end of the input`() {
        val text = "hello\n__________________________________"

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            hello
            <hr>
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should replace horizontal divider using ASCII scissors with an HR tag`() {
        val text =
            """
            hello
            -- %< -------------- >8 --
            world
            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            hello
            <hr>
            world
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtml() should wrap email signature in a DIV`() {
        val text =
            """
            text
            --${" "}
            signature with url: https://domain.example/
            """.trimIndent()

        val result = HtmlConverter.textToHtml(text)

        assertThat(result).isEqualTo(
            """
            <pre dir="auto" class="k9mail">
            text<br>
            <div class='k9mail-signature'>
            -- <br>
            signature with url: <a href="https://domain.example/">https://domain.example/</a>
            </div>
            </pre>
            """.trimIndent().removeLineBreaks()
        )
    }

    @Test
    fun `textToHtmlFragment() with single space at the start of a line`() {
        val text = " foo"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("\u00A0foo")
    }

    @Test
    fun `textToHtmlFragment() with two spaces at the start of a line`() {
        val text = "  foo"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("\u00A0 foo")
    }

    @Test
    fun `textToHtmlFragment() with consecutive spaces at the start of a line`() {
        val text = "    some words here"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("\u00A0\u00A0\u00A0 some words here")
    }

    @Test
    fun `textToHtmlFragment() with consecutive spaces between words`() {
        val text = "foo  bar"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("foo\u00A0 bar")
    }

    @Test
    fun `textToHtmlFragment() with single space at the end of a line`() {
        val text = "foo \n"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("foo <br>")
    }

    @Test
    fun `textToHtmlFragment() with consecutive spaces at the end of a line`() {
        val text = "some words here   \n"

        val result = HtmlConverter.textToHtmlFragment(text)

        assertThat(result).isEqualTo("some words here\u00A0\u00A0 <br>")
    }

    @Test
    fun `htmlToText() should convert BR tags to line breaks`() {
        val input =
            """
            One<br>
            Two<br>
            <br>
            Three
            """.trimIndent().removeLineBreaks()

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo(
            """
            One
            Two

            Three
            """.trimIndent()
        )
    }

    @Test
    fun `htmlToText() should insert line breaks after block elements`() {
        val input =
            """
            <p>One</p>
            <p>
            Two<br>
            Three
            </p>
            <div>Four</div>
            """.trimIndent().removeLineBreaks()

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo(
            """
            One

            Two
            Three
            
            Four
            """.trimIndent()
        )
    }

    @Test
    fun `htmlToText() should include link URIs`() {
        val input = "<a href='https://domain.example/'>Link text</a>"

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo("Link text <https://domain.example/>")
    }

    @Test
    fun `htmlToText() should not duplicate URI when link URI and text are the same`() {
        val input = "Text <a href='https://domain.example/path/'>https://domain.example/path/</a> more text"

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo("Text https://domain.example/path/ more text")
    }

    @Test
    fun `htmlToText() should not duplicate URI when the link text is just the link URI with some formatting`() {
        val input = "<a href='https://domain.example/path/'>https://<b>domain.example</b>/path/</a>"

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo("https://domain.example/path/")
    }

    @Test
    fun `htmlToText() should strip line breaks`() {
        val input =
            """
            One
            Two
            Three
            """.trimIndent()

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo("One Two Three")
    }

    @Test
    fun `htmlToText() with long text line should not add line breaks to output`() {
        val input =
            """
            |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam sit amet finibus felis,
            | viverra ullamcorper justo. Suspendisse potenti. Etiam erat sem, interdum a condimentum quis,
            | fringilla quis orci.
            """.trimMargin().removeLineBreaks()

        val result = HtmlConverter.htmlToText(input)

        assertThat(result).isEqualTo(input)
    }
}
