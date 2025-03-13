package com.fsck.k9.mailstore

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.testing.assertk.body
import com.fsck.k9.mail.testing.assertk.bodyPart
import com.fsck.k9.mail.testing.assertk.bodyParts
import com.fsck.k9.mail.testing.assertk.mimeType
import com.fsck.k9.mail.testing.crlf
import java.io.ByteArrayInputStream
import org.junit.Test

class MimePartStreamParserTest {
    @Test
    fun innerMessage_DispositionInline() {
        val messageContent =
            """
            From: <x@example.org>
            To: <y@example.org>
            Subject: Testmail 1
            Content-Type: multipart/mixed; boundary=1
            
            --1
            Content-Type: text/plain
            
            some text in the first part
            --1
            Content-Type: message/rfc822; name="message"
            
            To: <z@example.org>
            Subject: Hi
            Date: now
            Content-Type: text/plain
            
            inner text
            --1--
            """.trimIndent().crlf()

        val bodyPart = MimePartStreamParser.parse(null, ByteArrayInputStream(messageContent.toByteArray()))

        assertThat(bodyPart.body).isInstanceOf<MimeMultipart>().all {
            bodyParts().hasSize(2)
            bodyPart(1).all {
                mimeType().isEqualTo("message/rfc822")
                body().isInstanceOf<MimeMessage>()
            }
        }
    }

    @Test
    fun innerMessage_dispositionAttachment() {
        val messageContent =
            """
            From: <x@example.org>
            To: <y@example.org>
            Subject: Testmail 2
            Content-Type: multipart/mixed; boundary=1
            
            --1
            Content-Type: text/plain
            
            some text in the first part
            --1
            Content-Type: message/rfc822; name="message"
            Content-Disposition: attachment
            
            To: <z@example.org>
            Subject: Hi
            Date: now
            Content-Type: text/plain
            
            inner text
            --1--
            """.trimIndent().crlf()

        val bodyPart = MimePartStreamParser.parse(null, ByteArrayInputStream(messageContent.toByteArray()))

        assertThat(bodyPart.body).isInstanceOf<MimeMultipart>().all {
            bodyParts().hasSize(2)
            bodyPart(1).all {
                mimeType().isEqualTo("message/rfc822")
                body().isInstanceOf<DeferredFileBody>()
            }
        }
    }
}
