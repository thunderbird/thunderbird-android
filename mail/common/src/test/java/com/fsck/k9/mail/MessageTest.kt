package com.fsck.k9.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.BinaryTempFileMessageBody
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mail.testing.crlf
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.Date
import java.util.TimeZone
import okio.Buffer
import org.apache.james.mime4j.util.MimeUtil
import org.junit.After
import org.junit.Before
import org.junit.Test

class MessageTest {
    private lateinit var tempDirectory: File
    private var mimeBoundary: Int = 0

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        tempDirectory = Files.createTempDirectory("MessageTest").toFile()
        BinaryTempFileBody.setTempDirectory(tempDirectory)
    }

    @After
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun setSendDate_shouldSetSendDate() {
        val message = sampleMessage()
        val date = Date(0L)

        message.setSentDate(date, false)

        assertThat(message.sentDate).isEqualTo(date)
    }

    @Test
    fun setSendDate_withoutHidingTimeZone_shouldCreateDateHeaderWithTimezone() {
        val message = sampleMessage()

        message.setSentDate(Date(0L), false)

        assertThat(message.getFirstHeader("Date")).isEqualTo("Thu, 01 Jan 1970 09:00:00 +0900")
    }

    @Test
    fun setSendDate_withHidingTimeZone_shouldCreateDateHeaderWithTimezoneSetToZero() {
        val message = sampleMessage()

        message.setSentDate(Date(0L), true)

        assertThat(message.getFirstHeader("Date")).isEqualTo("Thu, 01 Jan 1970 00:00:00 +0000")
    }

    @Test
    fun writeTo_withNestedMessage() {
        mimeBoundary = 101
        val message = nestedMessage(nestedMessage(sampleMessage()))
        val out = ByteArrayOutputStream()

        message.writeTo(out)

        assertThat(out.toString()).isEqualTo(
            """
            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed;
             boundary=----Boundary103
            Content-Transfer-Encoding: 7bit

            ------Boundary103
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary103
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary103
            Content-Type: message/rfc822
            Content-Disposition: attachment
            Content-Transfer-Encoding: 7bit

            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed;
             boundary=----Boundary102
            Content-Transfer-Encoding: 7bit

            ------Boundary102
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary102
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary102
            Content-Type: message/rfc822
            Content-Disposition: attachment
            Content-Transfer-Encoding: 7bit

            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed;
             boundary=----Boundary101
            Content-Transfer-Encoding: 7bit

            ------Boundary101
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary101
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary101--

            ------Boundary102--

            ------Boundary103--

            """.trimIndent().crlf(),
        )
    }

    @Test
    fun toBodyPart() {
        mimeBoundary = 101
        val message = nestedMessage(nestedMessage(sampleMessage()))
        val out = ByteArrayOutputStream()
        val bodyPart = message.toBodyPart()

        bodyPart.writeTo(out)

        assertThat(out.toString()).isEqualTo(
            """
            Content-Type: multipart/mixed;
             boundary=----Boundary103
            Content-Transfer-Encoding: 7bit

            ------Boundary103
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary103
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary103
            Content-Type: message/rfc822
            Content-Disposition: attachment
            Content-Transfer-Encoding: 7bit

            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed;
             boundary=----Boundary102
            Content-Transfer-Encoding: 7bit

            ------Boundary102
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary102
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary102
            Content-Type: message/rfc822
            Content-Disposition: attachment
            Content-Transfer-Encoding: 7bit

            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed;
             boundary=----Boundary101
            Content-Transfer-Encoding: 7bit

            ------Boundary101
            Content-Type: text/plain;
             charset=utf-8
            Content-Transfer-Encoding: quoted-printable

            Testing=2E
            This is a text body with some greek characters=2E
            =CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8
            End of test=2E

            ------Boundary101
            Content-Type: application/octet-stream
            Content-Transfer-Encoding: base64

            ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

            ------Boundary101--

            ------Boundary102--

            ------Boundary103--

            """.trimIndent().crlf(),
        )
    }

    private fun sampleMessage(): MimeMessage {
        val message = MimeMessage().apply {
            setFrom(Address("from@example.com"))
            setHeader("To", "to@example.com")
            subject = "Test Message"
            setHeader("Date", "Wed, 28 Aug 2013 08:51:09 -0400")
            setEncoding(MimeUtil.ENC_7BIT)
        }

        val multipartBody = MimeMultipart("multipart/mixed", generateBoundary()).apply {
            addBodyPart(textBodyPart())
            addBodyPart(binaryBodyPart())
        }

        MimeMessageHelper.setBody(message, multipartBody)

        return message
    }

    private fun nestedMessage(subMessage: MimeMessage): MimeMessage {
        val tempMessageBody = BinaryTempFileMessageBody(MimeUtil.ENC_8BIT).apply {
            outputStream.use { subMessage.writeTo(it) }
        }

        val bodyPart = MimeBodyPart(tempMessageBody, "message/rfc822").apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment")
            setEncoding(MimeUtil.ENC_7BIT)
        }

        return sampleMessage().apply {
            val multipart = body as Multipart
            multipart.addBodyPart(bodyPart)
        }
    }

    private fun binaryBodyPart(): MimeBodyPart {
        val buffer = Buffer().writeUtf8("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n")
        val tempFileBody = BinaryTempFileBody(MimeUtil.ENC_BASE64).apply {
            outputStream.use { buffer.writeTo(it) }
        }

        return MimeBodyPart(tempFileBody, "application/octet-stream").apply {
            setEncoding(MimeUtil.ENC_BASE64)
        }
    }

    private fun textBodyPart(): MimeBodyPart {
        val textBody = TextBody(
            """
            Testing.
            This is a text body with some greek characters.
            αβγδεζηθ
            End of test.

            """.trimIndent().crlf(),
        )

        return MimeBodyPart().apply {
            MimeMessageHelper.setBody(this, textBody)
        }
    }

    private fun generateBoundary(): String {
        return "----Boundary${mimeBoundary++}"
    }
}

private fun Message.getFirstHeader(header: String): String = getHeader(header)[0]
