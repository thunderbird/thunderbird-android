package com.fsck.k9.mail.message

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import java.io.ByteArrayInputStream
import org.junit.Test

class MessageHeaderParserTest {
    @Test
    fun `parse should preserve UTF-8 attachment filename`() {
        // Arrange
        val expectedFilename = "Wspaniały świat.mp3"
        val headerData = "Content-Disposition: attachment; filename=\"$expectedFilename\"\r\n\r\n"
        val header = MimeHeader()

        // Act
        MessageHeaderParser.parse(ByteArrayInputStream(headerData.toByteArray(Charsets.UTF_8))) { name, raw ->
            header.addRawHeader(name, raw)
        }
        val contentDisposition = header.getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION)
        val filename = MimeUtility.getHeaderParameter(contentDisposition, "filename")

        // Assert
        assertThat(filename).isEqualTo(expectedFilename)
    }
}
