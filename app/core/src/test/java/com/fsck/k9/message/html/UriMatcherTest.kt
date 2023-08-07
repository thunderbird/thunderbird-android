package com.fsck.k9.message.html

import assertk.Assert
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import org.junit.Test

class UriMatcherTest {
    @Test
    fun emptyText() {
        assertNoMatch("")
    }

    @Test
    fun textWithoutUri() {
        assertNoMatch("some text here")
    }

    @Test
    fun simpleUri() {
        assertUrisFound("http://example.org", "http://example.org")
    }

    @Test
    fun uriPrecededBySpace() {
        assertUrisFound(" http://example.org", "http://example.org")
    }

    @Test
    fun uriPrecededByTab() {
        assertUrisFound("\thttp://example.org", "http://example.org")
    }

    @Test
    fun uriPrecededByOpeningParenthesis() {
        assertUrisFound("(http://example.org", "http://example.org")
    }

    @Test
    fun uriPrecededBySomeText() {
        assertUrisFound("Check out my fantastic URI: http://example.org", "http://example.org")
    }

    @Test
    fun uriWithTrailingText() {
        assertUrisFound("http://example.org/ is the best", "http://example.org/")
    }

    @Test
    fun uriEmbeddedInText() {
        assertUrisFound("prefix http://example.org/ suffix", "http://example.org/")
    }

    @Test
    fun uriWithUppercaseScheme() {
        assertUrisFound("HTTP://example.org/", "HTTP://example.org/")
    }

    @Test
    fun uriNotPrecededByValidSeparator() {
        assertNoMatch("myhttp://example.org")
    }

    @Test
    fun uriNotPrecededByValidSeparatorFollowedByValidUri() {
        assertUrisFound("myhttp: http://example.org", "http://example.org")
    }

    @Test
    fun schemaMatchWithInvalidUriInMiddleOfTextFollowedByValidUri() {
        assertUrisFound("prefix http:42 http://example.org", "http://example.org")
    }

    @Test
    fun multipleValidUrisInRow() {
        assertUrisFound(
            "prefix http://uri1.example.org some text http://uri2.example.org/path postfix",
            "http://uri1.example.org",
            "http://uri2.example.org/path",
        )
    }

    @Test
    fun `valid URIs`() {
        assertThat("http://domain.example").isValidUri()
        assertThat("https://domain.example/").isValidUri()
        assertThat("mailto:user@domain.example").isValidUri()
    }

    @Test
    fun `not URIs`() {
        assertThat("some text here").isNotValidUri()
        assertThat("some text including the string http:").isNotValidUri()
        assertThat("some text including an email address without URI scheme: user@domain.example").isNotValidUri()
    }

    @Test
    fun `valid URIs surrounded by other characters`() {
        assertThat("text https://domain.example/").isNotValidUri()
        assertThat("https://domain.example/ text").isNotValidUri()
        assertThat("<https://domain.example/>").isNotValidUri()
    }

    private fun assertNoMatch(text: String) {
        val uriMatches = UriMatcher.findUris(text)
        assertThat(uriMatches).isEmpty()
    }

    private fun assertUrisFound(text: String, vararg uris: String) {
        val uriMatches = UriMatcher.findUris(text)
        assertThat(uriMatches).hasSize(uris.size)
        var i = 0
        val end = uris.size
        while (i < end) {
            val uri = uris[i]
            val startIndex = text.indexOf(uri)
            assertThat(startIndex).isNotEqualTo(-1)
            val uriMatch = uriMatches[i]
            assertThat(uriMatch.startIndex).isEqualTo(startIndex)
            assertThat(uriMatch.endIndex).isEqualTo(startIndex + uri.length)
            assertThat(uriMatch.uri).isEqualTo(uri)
            i++
        }
    }
}

private fun Assert<String>.isValidUri() = given { actual ->
    assertThat(UriMatcher.isValidUri(actual)).isTrue()
}

private fun Assert<String>.isNotValidUri() = given { actual ->
    assertThat(UriMatcher.isValidUri(actual)).isFalse()
}
