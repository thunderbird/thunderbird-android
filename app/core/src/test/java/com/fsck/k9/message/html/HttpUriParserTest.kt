package com.fsck.k9.message.html

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.Test

class HttpUriParserTest {
    private val parser = HttpUriParser()

    @Test
    fun `missing domain`() {
        assertInvalidUri("http://")
    }

    @Test
    fun `missing domain followed by slash`() {
        assertInvalidUri("http:///")
    }

    @Test
    fun `simple domain`() {
        assertValidUri("http://www.google.com")
    }

    @Test
    fun `simple domain with https`() {
        assertValidUri("https://www.google.com")
    }

    @Test
    fun `simple RTSP URI`() {
        assertValidUri("rtsp://example.com/media.mp4")
    }

    @Test
    fun `subdomain starting with invalid character`() {
        assertInvalidUri("http://-www.google.com")
    }

    @Test
    fun `domain with trailing slash`() {
        assertValidUri("http://www.google.com/")
    }

    @Test
    fun `domain with user info`() {
        assertValidUri("http://test@google.com/")
    }

    @Test
    fun `domain with full user info`() {
        assertValidUri("http://test:secret@google.com/")
    }

    @Test
    fun `domain without www`() {
        assertValidUri("http://google.com/")
    }

    @Test
    fun query() {
        assertValidUri("http://google.com/give/me/?q=mode&c=information")
    }

    @Test
    fun fragment() {
        assertValidUri("http://google.com/give/me#only-the-best")
    }

    @Test
    fun `query and fragment`() {
        assertValidUri("http://google.com/give/me/?q=mode&c=information#only-the-best")
    }

    @Test
    fun `IPv4 address`() {
        assertValidUri("http://127.0.0.1")
    }

    @Test
    fun `IPv4 address with trailing slash`() {
        assertValidUri("http://127.0.0.1/")
    }

    @Test
    fun `IPv4 address with empty port`() {
        assertValidUri("http://127.0.0.1:")
    }

    @Test
    fun `IPv4 address with port`() {
        assertValidUri("http://127.0.0.1:524/")
    }

    @Test
    fun `IPv6 address`() {
        assertValidUri("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]")
    }

    @Test
    fun `IPv6 address with port`() {
        assertValidUri("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80")
    }

    @Test
    fun `IPv6 address with trailing slash`() {
        assertValidUri("http://[1080:0:0:0:8:800:200C:417A]/")
    }

    @Test
    fun `IPv6 address with end compression`() {
        assertValidUri("http://[3ffe:2a00:100:7031::1]")
    }

    @Test
    fun `IPv6 address with begin compression`() {
        assertValidUri("http://[1080::8:800:200C:417A]/")
    }

    @Test
    fun `IPv6 address with compression port`() {
        assertValidUri("http://[::FFFF:129.144.52.38]:80/")
    }

    @Test
    fun `IPv6 address with prepended compression`() {
        assertValidUri("http://[::192.9.5.5]/")
    }

    @Test
    fun `IPv6 address with trailing IP4 and port`() {
        assertValidUri("http://[::192.9.5.5]:80/")
    }

    @Test
    fun `IPv6 without closing square bracket`() {
        assertInvalidUri("http://[1080:0:0:0:8:80:200C:417A/")
    }

    @Test
    fun `IPv6 invalid closing square bracket`() {
        assertInvalidUri("http://[1080:0:0:0:8:800:270C:417A/]")
    }

    @Test
    fun `domain with trailing space`() {
        val text = "http://google.com/ "

        val uriMatch = parser.parseUri(text, 0)

        assertUriMatch("http://google.com/", uriMatch)
    }

    @Test
    fun `domain with trailing newline`() {
        val text = "http://google.com/\n"

        val uriMatch = parser.parseUri(text, 0)

        assertUriMatch("http://google.com/", uriMatch)
    }

    @Test
    fun `domain with trailing angle bracket`() {
        val text = "<http://google.com/>"

        val uriMatch = parser.parseUri(text, 1)

        assertUriMatch("http://google.com/", uriMatch, 1)
    }

    @Test
    fun `URI at the end of input`() {
        val prefix = "prefix "
        val uri = "http://google.com/"
        val text = prefix + uri

        val uriMatch = parser.parseUri(text, prefix.length)

        assertUriMatch("http://google.com/", uriMatch, prefix.length)
    }

    @Test
    fun `URI in middle of input`() {
        val prefix = "prefix "
        val uri = "http://google.com/"
        val postfix = " postfix"
        val text = prefix + uri + postfix

        val uriMatch = parser.parseUri(text, prefix.length)

        assertUriMatch("http://google.com/", uriMatch, prefix.length)
    }

    @Test
    fun `URI wrapped in parentheses`() {
        val input = "(https://domain.example/)"

        val uriMatch = parser.parseUri(input, 1)

        assertUriMatch("https://domain.example/", uriMatch, 1)
    }

    @Test
    fun `URI containing parentheses`() {
        val input = "https://domain.example/(parentheses)"

        val uriMatch = parser.parseUri(input, 0)

        assertUriMatch("https://domain.example/(parentheses)", uriMatch, 0)
    }

    @Test
    fun `URI containing parentheses wrapped in parentheses`() {
        val input = "(https://domain.example/(parentheses))"

        val uriMatch = parser.parseUri(input, 1)

        assertUriMatch("https://domain.example/(parentheses)", uriMatch, 1)
    }

    @Test
    fun `URI ending in dot at end of text`() {
        val input = "URL: https://domain.example/path."

        val uriMatch = parser.parseUri(input, 5)

        assertUriMatch("https://domain.example/path", uriMatch, 5)
    }

    @Test
    fun `URI ending in dot with additional text`() {
        val input = "URL: https://domain.example/path. Some other text"
        val uriMatch = parser.parseUri(input, 5)
        assertUriMatch("https://domain.example/path", uriMatch, 5)
    }

    @Test
    fun `URI wrapped in angle brackets ending in dot`() {
        val input = "URL: <https://domain.example/path.>"

        val uriMatch = parser.parseUri(input, 6)

        assertUriMatch("https://domain.example/path.", uriMatch, 6)
    }

    @Test
    fun `URI wrapped in parentheses ending in dot`() {
        val input = "URL: (https://domain.example/path.)"

        val uriMatch = parser.parseUri(input, 6)

        assertUriMatch("https://domain.example/path.", uriMatch, 6)
    }

    @Test
    fun `URI wrapped in parentheses followed by a dot`() {
        val input = "URL: (https://domain.example/path)."

        val uriMatch = parser.parseUri(input, 6)

        assertUriMatch("https://domain.example/path", uriMatch, 6)
    }

    @Test
    fun `URI wrapped in parentheses followed by a dot and some other text`() {
        val input = "URL: (https://domain.example/path). Some other text"

        val uriMatch = parser.parseUri(input, 6)

        assertUriMatch("https://domain.example/path", uriMatch, 6)
    }

    @Test
    fun `URI wrapped in parentheses followed by a question mark and some other text`() {
        val input = "URL: (https://domain.example/path)? Some other text"

        val uriMatch = parser.parseUri(input, 6)

        assertUriMatch("https://domain.example/path", uriMatch, 6)
    }

    @Test
    fun `negative 'startPos' value`() {
        assertFailure {
            parser.parseUri("test", -1)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Invalid 'startPos' value")
    }

    @Test
    fun `out of bounds 'startPos' value`() {
        assertFailure {
            parser.parseUri("test", 4)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Invalid 'startPos' value")
    }

    private fun assertValidUri(uri: String) {
        val uriMatch = parser.parseUri(uri, 0)

        assertUriMatch(uri, uriMatch)
    }

    private fun assertUriMatch(uri: String, uriMatch: UriMatch?, offset: Int = 0) {
        assertThat(uriMatch).isNotNull().isEqualTo(
            UriMatch(
                startIndex = offset,
                endIndex = offset + uri.length,
                uri = uri,
            ),
        )
    }

    private fun assertInvalidUri(uri: String) {
        val uriMatch = parser.parseUri(uri, 0)
        assertThat(uriMatch).isNull()
    }
}
