package com.fsck.k9.message.extractors

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.message.MessageCreationHelper.createEmptyPart
import com.fsck.k9.message.MessageCreationHelper.createMultipart
import com.fsck.k9.message.MessageCreationHelper.createPart
import com.fsck.k9.message.MessageCreationHelper.createTextPart
import org.junit.Test

class TextPartFinderTest {
    private val textPartFinder = TextPartFinder()

    @Test
    fun `text_plain part`() {
        val part = createTextPart("text/plain")

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(part)
    }

    @Test
    fun `text_html part`() {
        val part = createTextPart("text/html")

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(part)
    }

    @Test
    fun `without text part`() {
        val part = createPart("image/jpeg")

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isNull()
    }

    @Test
    fun `multipart_alternative text_plain and text_html`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/alternative",
            expected,
            createTextPart("text/html"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_alternative containing text_html and text_plain`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/alternative",
            createTextPart("text/html"),
            expected,
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_alternative containing multiple text_html parts`() {
        val expected = createTextPart("text/html")
        val part = createMultipart(
            "multipart/alternative",
            createPart("image/gif"),
            expected,
            createTextPart("text/html"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_alternative not containing any text parts`() {
        val part = createMultipart(
            "multipart/alternative",
            createPart("image/gif"),
            createPart("application/pdf"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isNull()
    }

    @Test
    fun `multipart_alternative containing multipart_related containing text_plain`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/alternative",
            createMultipart(
                "multipart/related",
                expected,
                createPart("image/jpeg"),
            ),
            createTextPart("text/html"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_alternative containing multipart_related and text_plain`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/alternative",
            createMultipart(
                "multipart/related",
                createTextPart("text/html"),
                createPart("image/jpeg"),
            ),
            expected,
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed containing text_plain`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/mixed",
            createPart("image/jpeg"),
            expected,
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed containing text_html and text_plain`() {
        val expected = createTextPart("text/html")
        val part = createMultipart(
            "multipart/mixed",
            expected,
            createTextPart("text/plain"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed not containing any text parts`() {
        val part = createMultipart(
            "multipart/mixed",
            createPart("image/jpeg"),
            createPart("image/gif"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isNull()
    }

    @Test
    fun `multipart_mixed containing multipart_alternative containing text_plain and text_html`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/mixed",
            createPart("image/jpeg"),
            createMultipart(
                "multipart/alternative",
                expected,
                createTextPart("text/html"),
            ),
            createTextPart("text/plain"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed containing multipart_alternative containing text_html and text_plain`() {
        val expected = createTextPart("text/plain")
        val part = createMultipart(
            "multipart/mixed",
            createMultipart(
                "multipart/alternative",
                createTextPart("text/html"),
                expected,
            ),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_alternative containing empty text_plain and text_html`() {
        val expected = createEmptyPart("text/plain")
        val part = createMultipart(
            "multipart/alternative",
            expected,
            createTextPart("text/html"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed containing empty text_html and text_plain`() {
        val expected = createEmptyPart("text/html")
        val part = createMultipart(
            "multipart/mixed",
            expected,
            createTextPart("text/plain"),
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `multipart_mixed containing multipart_alternative and text_plain`() {
        val expected = createEmptyPart("text/plain")
        val part = createMultipart(
            "multipart/mixed",
            createMultipart(
                "multipart/alternative",
                createPart("image/jpeg"),
                createPart("image/png"),
            ),
            expected,
        )

        val result = textPartFinder.findFirstTextPart(part)

        assertThat(result).isEqualTo(expected)
    }
}
