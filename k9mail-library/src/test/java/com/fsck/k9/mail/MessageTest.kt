package com.fsck.k9.mail


import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.*
import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.apache.james.mime4j.util.MimeUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.util.*


@RunWith(K9LibRobolectricTestRunner::class)
class MessageTest {
    private val context = RuntimeEnvironment.application
    private var mimeBoundary: Int = 0

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        BinaryTempFileBody.setTempDirectory(context.cacheDir)
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

        assertThat(out.toString()).isEqualTo("""
            From: from@example.com
            To: to@example.com
            Subject: Test Message
            Date: Wed, 28 Aug 2013 08:51:09 -0400
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="----Boundary103"
            Content-Transfer-Encoding: 7bit

            ------Boundary103
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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
            Content-Type: multipart/mixed; boundary="----Boundary102"
            Content-Transfer-Encoding: 7bit

            ------Boundary102
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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
            Content-Type: multipart/mixed; boundary="----Boundary101"
            Content-Transfer-Encoding: 7bit

            ------Boundary101
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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

            """.trimIndent().crlf())
    }

    @Test
    fun toBodyPart() {
        mimeBoundary = 101
        val message = nestedMessage(nestedMessage(sampleMessage()))
        val out = ByteArrayOutputStream()
        val bodyPart = message.toBodyPart()

        bodyPart.writeTo(out)

        assertThat(out.toString()).isEqualTo("""
            Content-Type: multipart/mixed; boundary="----Boundary103"
            Content-Transfer-Encoding: 7bit

            ------Boundary103
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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
            Content-Type: multipart/mixed; boundary="----Boundary102"
            Content-Transfer-Encoding: 7bit

            ------Boundary102
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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
            Content-Type: multipart/mixed; boundary="----Boundary101"
            Content-Transfer-Encoding: 7bit

            ------Boundary101
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain;
             charset=utf-8

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

            """.trimIndent().crlf())
    }


    private fun sampleMessage(): MimeMessage {
        val message = MimeMessage().apply {
            setFrom(Address("from@example.com"))
            setRecipient(RecipientType.TO, Address("to@example.com"))
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
        val textBody = TextBody("""
            Testing.
            This is a text body with some greek characters.
            αβγδεζηθ
            End of test.

            """.trimIndent().crlf()
        ).apply {
            setCharset("utf-8")
        }

        return MimeBodyPart().apply {
            MimeMessageHelper.setBody(this, textBody)
            CharsetSupport.setCharset("utf-8", this)
        }
    }

    private fun generateBoundary(): String {
        return "----Boundary${mimeBoundary++}"
    }
}

private fun Message.getFirstHeader(header: String): String = getHeader(header)[0]

private fun String.crlf() = replace("\n", "\r\n")
