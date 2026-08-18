package net.thunderbird.feature.mail.message.composer.signature

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DefaultHtmlSignatureSanitizerTest {

    private val testSubject = DefaultHtmlSignatureSanitizer()

    @Test
    fun `sanitize should return empty string when input is empty`() {
        // Arrange
        val input = ""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `sanitize should return input unchanged when input is plain text`() {
        // Arrange
        val input = "Just some text"

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).isEqualTo("Just some text")
    }

    @Test
    fun `sanitize should keep basic formatting tags`() {
        // Arrange
        // language=html
        val input = "<p>Hello <b>world</b></p>"

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        // language=html
        assertThat(result).contains("<b>world</b>")
    }

    @Test
    fun `sanitize should keep anchor when href uses https protocol`() {
        // Arrange
        // language=html
        val input = """<a href="https://example.com">example</a>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        // language=html
        assertThat(result).contains("""href="https://example.com"""")
        // language=html
        assertThat(result).contains(">example</a>")
    }

    @Test
    fun `sanitize should keep anchor when href uses mailto protocol`() {
        // Arrange
        // language=html
        val input = """<a href="mailto:someone@example.com">mail me</a>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).contains("""href="mailto:someone@example.com"""")
    }

    @Test
    fun `sanitize should keep image when src uses https protocol`() {
        // Arrange
        // language=html
        val input = """<img src="https://example.com/logo.png" alt="logo">"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).contains("""src="https://example.com/logo.png"""")
        assertThat(result).contains("""alt="logo"""")
    }

    @Test
    fun `sanitize should keep image when src uses cid protocol`() {
        // Arrange
        // language=html
        val input = """<img src="cid:logo@example.com" alt="">"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        // language=html
        assertThat(result).contains("""src="cid:logo@example.com"""")
    }

    @Test
    fun `sanitize should keep inline style attribute`() {
        // Arrange
        // language=html
        val input = """<span style="color: red;">red</span>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        // language=html
        assertThat(result).contains("""style="color: red;"""")
    }

    @Test
    fun `sanitize should remove script element`() {
        // Arrange
        // language=html
        val input = "<p>Hi</p><script>alert('xss')</script>"

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).contains("<p>Hi</p>")
        assertThat(result).doesNotContain("script")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `sanitize should remove iframe element`() {
        // Arrange
        // language=html
        val input = """<iframe src="https://evil.example"></iframe>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).doesNotContain("iframe")
    }

    @Test
    fun `sanitize should remove object element`() {
        // Arrange
        // language=html
        val input = """<object data="https://evil.example/payload"></object>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).doesNotContain("object")
    }

    @Test
    fun `sanitize should remove embed element`() {
        // Arrange
        // language=html
        val input = """<embed src="https://evil.example/payload">"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).doesNotContain("embed")
    }

    @Test
    fun `sanitize should remove inline event handler attribute`() {
        // Arrange
        // language=html
        val input = """<a href="https://example.com" onclick="alert(1)">click</a>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        // language=html
        assertThat(result).contains("""href="https://example.com"""")
        assertThat(result).doesNotContain("onclick")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `sanitize should remove href when anchor uses javascript protocol`() {
        // Arrange
        // language=html
        val input = """<a href="javascript:alert(1)">click</a>"""

        // Act
        val result = testSubject.sanitize(input)

        // Assert
        assertThat(result).doesNotContain("javascript")
        assertThat(result).doesNotContain("alert")
    }
}
