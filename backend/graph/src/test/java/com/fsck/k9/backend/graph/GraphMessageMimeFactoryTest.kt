package com.fsck.k9.backend.graph

import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.MimeUtility
import java.io.File
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.legacy.logging.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GraphMessageMimeFactoryTest {
    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
        Log.logger = TestLogger()
    }

    @Test
    fun `recipient display name with punctuation is preserved when parsing MIME`() {
        val recipientName = "Smith, Jr."
        val message = GraphMessageMimeFactory.create(
            message = graphMessage(toRecipient = GraphEmailAddress(recipientName, "smith@example.com")),
            attachments = emptyList(),
        )

        val recipient = message.getRecipients(RecipientType.TO).single()

        assertEquals("smith@example.com", recipient.address)
        assertEquals(recipientName, recipient.personal)
    }

    @Test
    fun `attachment size is available through the standard MIME size parameter`() {
        val message = GraphMessageMimeFactory.create(
            message = graphMessage(toRecipient = GraphEmailAddress("A user", "user@example.com")),
            attachments = listOf(
                GraphAttachment(
                    id = "attachment-1",
                    name = "notes.txt",
                    contentType = "text/plain",
                    size = 42,
                    isInline = false,
                    contentId = null,
                    type = "#microsoft.graph.fileAttachment",
                ),
            ),
        )

        val attachmentPart = (message.body as MimeMultipart).getBodyPart(1)

        assertEquals("42", MimeUtility.getHeaderParameter(attachmentPart.disposition, "size"))
        val encodedAttachmentId = attachmentPart.getHeader(GraphMessageMimeFactory.ATTACHMENT_ID_HEADER).single()
        assertEquals(encodedAttachmentId, attachmentPart.serverExtra)
        assertEquals("attachment-1", GraphMessageMimeFactory.decodeAttachmentId(attachmentPart.serverExtra))
        assertNull(attachmentPart.body)
    }

    @Test
    fun `reference attachments are omitted because Graph does not expose raw bytes`() {
        val message = GraphMessageMimeFactory.create(
            message = graphMessage(toRecipient = GraphEmailAddress("A user", "user@example.com")),
            attachments = listOf(
                GraphAttachment(
                    id = "reference-1",
                    name = "cloud-file.txt",
                    contentType = "text/plain",
                    size = 42,
                    isInline = false,
                    contentId = null,
                    type = "#microsoft.graph.referenceAttachment",
                ),
            ),
        )

        assertEquals(1, (message.body as MimeMultipart).bodyParts.size)
    }

    private fun graphMessage(toRecipient: GraphEmailAddress) = GraphMessage(
        id = "message-1",
        receivedDateTime = null,
        sentDateTime = null,
        lastModifiedDateTime = null,
        subject = null,
        internetMessageId = null,
        from = null,
        sender = null,
        toRecipients = listOf(toRecipient),
        ccRecipients = emptyList(),
        bccRecipients = emptyList(),
        replyTo = emptyList(),
        bodyContent = "",
        bodyContentType = "text",
        bodyPreview = null,
        isRead = false,
        isFlagged = false,
        hasAttachments = false,
        removed = false,
    )
}
