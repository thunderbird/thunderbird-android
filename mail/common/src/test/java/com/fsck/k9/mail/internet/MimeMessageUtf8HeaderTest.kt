package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayInputStream
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MimeMessageUtf8HeaderTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(tempFolder.root)
    }

    @Test
    fun `parse should preserve UTF-8 attachment filename`() {
        // Arrange
        val expectedFilename = "Wspaniały świat.mp3"
        val messageData =
            """
            Content-Type: audio/mpeg
            Content-Disposition: attachment; filename="$expectedFilename"

            attachment content
            """.trimIndent()

        // Act
        val testSubject = MimeMessage.parseMimeMessage(
            ByteArrayInputStream(messageData.toByteArray(Charsets.UTF_8)),
            false,
        )
        val filename = MimeUtility.getHeaderParameter(testSubject.disposition, "filename")

        // Assert
        assertThat(filename).isEqualTo(expectedFilename)
    }
}
