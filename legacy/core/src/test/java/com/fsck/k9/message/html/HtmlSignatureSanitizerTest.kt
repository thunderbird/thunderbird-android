package com.fsck.k9.message.html

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class HtmlSignatureSanitizerTest {

    @Test
    fun `empty input returns empty string`() {
        assertThat(HtmlSignatureSanitizer.sanitize("")).isEmpty()
    }

    @Test
    fun `preserves basic formatting tags`() {
        val input = "<p>Hello <b>world</b></p>"
        assertThat(HtmlSignatureSanitizer.sanitize(input)).contains("<b>world</b>")
    }

    @Test
    fun `preserves anchors with https urls`() {
        val input = """<a href="https://example.com">example</a>"""
        val result = HtmlSignatureSanitizer.sanitize(input)
        assertThat(result).contains("""href="https://example.com"""")
        assertThat(result).contains(">example</a>")
    }

    @Test
    fun `preserves img with https src`() {
        val input = """<img src="https://example.com/logo.png" alt="logo">"""
        val result = HtmlSignatureSanitizer.sanitize(input)
        assertThat(result).contains("""src="https://example.com/logo.png"""")
    }

    @Test
    fun `preserves inline style attributes`() {
        val input = """<span style="color: red;">red</span>"""
        assertThat(HtmlSignatureSanitizer.sanitize(input)).contains("""style="color: red;"""")
    }

    @Test
    fun `strips script elements`() {
        val input = "<p>Hi</p><script>alert('xss')</script>"
        val result = HtmlSignatureSanitizer.sanitize(input)
        assertThat(result).doesNotContain("script")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `strips inline event handler attributes`() {
        val input = """<a href="https://example.com" onclick="alert(1)">click</a>"""
        val result = HtmlSignatureSanitizer.sanitize(input)
        assertThat(result).doesNotContain("onclick")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `strips javascript urls from anchors`() {
        val input = """<a href="javascript:alert(1)">click</a>"""
        val result = HtmlSignatureSanitizer.sanitize(input)
        assertThat(result).doesNotContain("javascript")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `strips iframe elements`() {
        val input = """<iframe src="https://evil.example"></iframe>"""
        assertThat(HtmlSignatureSanitizer.sanitize(input)).doesNotContain("iframe")
    }

    @Test
    fun `plain text passes through unchanged`() {
        val input = "Just some text"
        assertThat(HtmlSignatureSanitizer.sanitize(input)).isEqualTo("Just some text")
    }
}
