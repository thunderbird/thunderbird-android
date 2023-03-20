package com.fsck.k9.message.extractors

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeBodyPart
import org.junit.Test

class BasicPartInfoExtractorTest {
    private val basicPartInfoExtractor = BasicPartInfoExtractor()

    @Test
    fun `extractPartInfo with 'filename' parameter in Content-Disposition header`() {
        val part = createPart(
            contentType = "application/octet-stream",
            contentDisposition = "attachment; filename=\"attachment_name.txt\"; size=23",
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("attachment_name.txt")
        assertThat(partInfo.size).isEqualTo(23L)
    }

    @Test
    fun `extractPartInfo with 'name' parameter in Content-Type header`() {
        val part = createPart(
            contentType = "image/jpeg; name=\"attachment.jpeg\"",
            contentDisposition = "attachment; size=42",
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("attachment.jpeg")
        assertThat(partInfo.size).isEqualTo(42L)
    }

    @Test
    fun `extractPartInfo without display name and size`() {
        val part = createPart(contentType = "text/plain", contentDisposition = "attachment")

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("noname.txt")
        assertThat(partInfo.size).isNull()
    }

    @Test
    fun `extractPartInfo without display name and unknown mime type`() {
        val part = createPart(contentType = "x-made-up/unknown", contentDisposition = "attachment")

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("noname")
        assertThat(partInfo.size).isNull()
    }

    @Test
    fun `extractPartInfo with missing Content-Disposition header`() {
        val part = createPart(
            contentType = "application/octet-stream; name=\"attachment.dat\"",
            contentDisposition = null,
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("attachment.dat")
        assertThat(partInfo.size).isNull()
    }

    @Test
    fun `extractPartInfo with missing Content-Disposition header and name`() {
        val part = createPart(
            contentType = "application/octet-stream",
            contentDisposition = null,
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("noname")
        assertThat(partInfo.size).isNull()
    }

    @Test
    fun `extractPartInfo without any relevant headers`() {
        val part = createPart(
            contentType = null,
            contentDisposition = null,
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("noname.txt")
        assertThat(partInfo.size).isNull()
    }

    @Test
    fun `extractPartInfo with invalid Content-Disposition header`() {
        val part = createPart(
            contentType = "application/octet-stream",
            contentDisposition = "something; <invalid>",
        )

        val partInfo = basicPartInfoExtractor.extractPartInfo(part)

        assertThat(partInfo.displayName).isEqualTo("noname")
        assertThat(partInfo.size).isNull()
    }

    private fun createPart(contentType: String?, contentDisposition: String?): Part {
        return MimeBodyPart().apply {
            if (contentType != null) {
                addHeader("Content-Type", contentType)
            }

            if (contentDisposition != null) {
                addHeader("Content-Disposition", contentDisposition)
            }
        }
    }
}
