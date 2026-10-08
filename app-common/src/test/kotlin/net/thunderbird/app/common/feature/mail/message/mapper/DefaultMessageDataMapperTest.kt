package net.thunderbird.app.common.feature.mail.message.mapper

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.internet.AddressHeaderBuilder
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.BinaryMemoryBody
import java.io.File
import java.util.Date
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.app.common.feature.mail.message.domain.model.LegacyMessageSource
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageAddress
import net.thunderbird.feature.mail.message.MessageAttachment
import net.thunderbird.feature.mail.message.MessageAttachmentContentId
import net.thunderbird.feature.mail.message.MessageAttachmentDisplayName
import net.thunderbird.feature.mail.message.MessageBody
import net.thunderbird.feature.mail.message.MessageDownloadState
import net.thunderbird.feature.mail.message.MessageEnvelope
import net.thunderbird.feature.mail.message.MessageFlag
import net.thunderbird.feature.mail.message.MessageHeaderId
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.MimeType
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.robolectric.RuntimeEnvironment
import com.fsck.k9.mail.Message as LegacyMessage

class DefaultMessageDataMapperTest : RobolectricTest() {

    private val testLogger = TestLogger()
    private val context = RuntimeEnvironment.getApplication()
    private val testSubject: MessageDataMapper<LegacyMessage> = DefaultMessageDataMapper(
        logger = testLogger,
        attachmentResolver = AttachmentResolver(testLogger, context),
        context = context,
    )

    @Test
    fun `toDto should map domain envelope, headers and flags onto the legacy message`() = runTest {
        // Arrange
        val domain = buildDomainMessage()

        // Act
        val legacy = testSubject.toDto(domain)

        // Assert
        assertThat(legacy).all {
            prop(LegacyMessage::getAccountId).isEqualTo(AccountId)
            prop(LegacyMessage::getUid).isEqualTo(SERVER_ID)
            prop(LegacyMessage::getInternalDate).transform { it.toLocalDateTime() }.isEqualTo(ReceivedAt)
            prop(LegacyMessage::getSubject).isEqualTo(SUBJECT)
            prop(LegacyMessage::getFrom).isEqualTo(arrayOf(From.toLegacyAddress()))
            prop(LegacyMessage::getSender).isEmpty()
            prop(LegacyMessage::getReplyTo).isEqualTo(arrayOf(ReplyTo.toLegacyAddress()))
            transform { it.getRecipients(LegacyMessage.RecipientType.TO) }
                .isEqualTo(arrayOf(To.toLegacyAddress()))
            transform { it.getRecipients(LegacyMessage.RecipientType.CC) }.isEmpty()
            transform { it.getRecipients(LegacyMessage.RecipientType.BCC) }.isEmpty()
            prop(LegacyMessage::getSentDate).transform { it.toLocalDateTime() }.isEqualTo(SentAt)
            prop(LegacyMessage::getMessageId).isEqualTo(MessageId.value)
            transform { it.getHeader("In-Reply-To") }.isEqualTo(arrayOf(InReplyTo.value))
            prop(LegacyMessage::getReferences).isEqualTo(arrayOf(References.joinToString(" ") { it.value }))
            transform { it.getHeader(EXTRA_HEADER_NAME) }.isEqualTo(arrayOf(EXTRA_HEADER_VALUE))
            transform { it.isSet(Flag.SEEN) }.isTrue()
            transform { it.isSet(Flag.FLAGGED) }.isTrue()
            transform { it.isSet(Flag.ANSWERED) }.isFalse()
        }
    }

    @Test
    fun `toDomain should map legacy envelope, headers, flags and body onto the domain message`() = runTest {
        // Arrange
        val dto = buildLegacyMessage()

        // Act
        val result = testSubject.toDomain(dto)

        // Assert
        assertThat(result).isEqualTo(
            buildDomainMessage(
                // A bare legacy message isn't backed by a LocalMessage, so these can't be recovered.
                downloadState = MessageDownloadState.ENVELOPE,
            ).copy(source = LegacyMessageSource(dto)),
        )
    }

    @Test
    fun `toDto followed by toDomain should round trip a message preserving mappable fields`() = runTest {
        // Arrange
        val domain = buildDomainMessage(downloadState = MessageDownloadState.FULL)

        // Act
        val legacy = testSubject.toDto(domain)
        val result = testSubject.toDomain(legacy)

        // Assert
        assertThat(result).isEqualTo(
            domain.copy(
                // LocalStore-only concepts. A message built from scratch (not backed by a
                // LocalMessage) can't carry these across the round trip.
                id = null,
                folderId = null,
                threadRoot = null,
                size = null,
                downloadState = MessageDownloadState.ENVELOPE,
                source = LegacyMessageSource(legacy),
            ),
        )
    }

    @Test
    fun `toDomain should expose decoded bytes for a base64 encoded attachment`() = runTest {
        // Arrange
        val dto = buildLegacyMessage()
        val encodedBody = BinaryMemoryBody("QUJD".toByteArray(Charsets.US_ASCII), "base64")
        val attachment = MimeBodyPart.create(encodedBody, "application/octet-stream")
        attachment.setHeader("Content-Disposition", "attachment; filename=sample.bin")
        val mixed = MimeMultipart.newInstance().apply {
            addBodyPart(MimeBodyPart.create(dto.body))
            addBodyPart(attachment)
        }
        MimeMessageHelper.setBody(dto, mixed)

        // Act
        val result = testSubject.toDomain(dto)
        val mappedAttachment = requireNotNull(result.attachments).single()
        val file = File(requireNotNull(mappedAttachment.internalUri).toString().removePrefix("file:"))

        // Assert
        assertThat(file.readBytes()).isEqualTo("ABC".toByteArray(Charsets.US_ASCII))
        assertThat(mappedAttachment.size).isEqualTo(3L)
    }

    @Test
    fun `toDomain should not include case variant of known header in extra headers`() = runTest {
        // Arrange
        val dto = buildLegacyMessage()
        dto.setHeader("subject", SUBJECT)

        // Act
        val result = testSubject.toDomain(dto)

        // Assert
        assertThat(result.headers.extra).isEqualTo(mapOf(EXTRA_HEADER_NAME to EXTRA_HEADER_VALUE))
    }

    @Test
    fun `toDto followed by toDomain should preserve an inline image content id`() = runTest {
        // Arrange
        val inline = MessageAttachment.Inline(
            mimeType = MimeType("image/png"),
            displayName = MessageAttachmentDisplayName("logo.png"),
            size = 0,
            internalUri = null,
            contentId = MessageAttachmentContentId("logo@example.com"),
        )
        val domain = buildDomainMessage().copy(attachments = listOf(inline))

        // Act
        val result = testSubject.toDomain(testSubject.toDto(domain))

        // Assert
        val mappedInline = requireNotNull(result.attachments).single() as MessageAttachment.Inline
        assertThat(mappedInline.contentId).isEqualTo(inline.contentId)
        assertThat(mappedInline.displayName).isEqualTo(inline.displayName)
    }

    @Test
    fun `toDomain should not leave multiple cached copies when mapping the same attachment twice`() = runTest {
        // Arrange
        val dto = buildLegacyMessage()
        val attachment = MimeBodyPart.create(
            BinaryMemoryBody("ABC".toByteArray(Charsets.US_ASCII), "8bit"),
            "application/octet-stream",
        )
        attachment.setHeader("Content-Disposition", "attachment; filename=sample.bin")
        MimeMessageHelper.setBody(dto, MimeMultipart.newInstance().apply { addBodyPart(attachment) })
        // Act
        val first = testSubject.toDomain(dto)
        val second = testSubject.toDomain(dto)
        val firstUri = requireNotNull(first.attachments).single().internalUri
        val secondUri = requireNotNull(second.attachments).single().internalUri
        val cached = context.cacheDir.listFiles { f -> f.name.startsWith("attachment") }.orEmpty()

        // Assert
        assertThat(firstUri).isEqualTo(secondUri)
        assertThat(cached.size).isEqualTo(1)
    }

    private fun buildDomainMessage(
        downloadState: MessageDownloadState = MessageDownloadState.FULL,
    ): Message = Message(
        id = null,
        serverId = MessageServerId(SERVER_ID),
        accountId = AccountId,
        folderId = null,
        threadRoot = null,
        receivedAt = ReceivedAt,
        envelope = MessageEnvelope(
            subject = SUBJECT,
            from = listOf(From),
            sender = null,
            replyTo = listOf(ReplyTo),
            to = listOf(To),
            cc = emptyList(),
            bcc = emptyList(),
            sentAt = SentAt,
        ),
        headers = MessageHeaders(
            messageId = MessageId,
            references = References,
            inReplyTo = listOf(InReplyTo),
            // A header with no dedicated field on Message, so it's preserved verbatim.
            extra = mapOf(EXTRA_HEADER_NAME to EXTRA_HEADER_VALUE),
        ),
        body = MessageBody(preview = PLAIN_TEXT, html = HTML, plainText = PLAIN_TEXT),
        downloadState = downloadState,
        flags = setOf(MessageFlag.Read, MessageFlag.Starred),
        attachments = null,
        size = null,
    )

    private fun buildLegacyMessage(): LegacyMessage {
        val message = MimeMessage.create()
        message.setAccountId(AccountId)
        message.uid = SERVER_ID
        message.internalDate = ReceivedAt.toLegacyDate()
        message.setSubject(SUBJECT)
        message.setFrom(From.toLegacyAddress())
        message.replyTo = arrayOf(ReplyTo.toLegacyAddress())
        message.setHeader("To", AddressHeaderBuilder.createHeaderValue(arrayOf(To.toLegacyAddress())))
        message.setSentDate(SentAt.toLegacyDate(), false)
        message.setMessageId(MessageId.value)
        message.setInReplyTo(InReplyTo.value)
        message.setReferences(References.joinToString(" ") { it.value })
        message.setHeader(EXTRA_HEADER_NAME, EXTRA_HEADER_VALUE)
        message.setFlag(Flag.SEEN, true)
        message.setFlag(Flag.FLAGGED, true)

        val body = MimeMultipart(BoundaryGenerator.getInstance().generateBoundary()).apply {
            setSubType("alternative")
            addBodyPart(MimeBodyPart.create(TextBody(PLAIN_TEXT), "text/plain"))
            addBodyPart(MimeBodyPart.create(TextBody(HTML), "text/html"))
        }
        MimeMessageHelper.setBody(message, body)

        return message
    }

    private fun MessageAddress.toLegacyAddress(): Address = Address(value, label, false)

    private fun LocalDateTime.toLegacyDate(): Date =
        Date(toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())

    private fun Date.toLocalDateTime(): LocalDateTime {
        val timeZone = TimeZone.currentSystemDefault()
        return kotlin.time.Instant.fromEpochMilliseconds(time).toLocalDateTime(timeZone)
    }

    private companion object {
        const val SERVER_ID = "100"
        const val SUBJECT = "Test subject"
        const val PLAIN_TEXT = "Hello, this is a test message body."
        const val HTML = "<p>Hello, this is a test message body.</p>"

        const val EXTRA_HEADER_NAME = "X-Original-To"
        const val EXTRA_HEADER_VALUE = "someone@example.com"

        val AccountId = AccountIdFactory.of("11111111-1111-1111-1111-111111111111")
        val ReceivedAt = LocalDateTime(2024, 1, 15, 10, 30, 0)
        val SentAt = LocalDateTime(2024, 1, 15, 10, 25, 0)

        val From = MessageAddress(value = "alice@example.com", label = "Alice")
        val To = MessageAddress(value = "bob@example.com", label = "Bob")
        val ReplyTo = MessageAddress(value = "reply@example.com", label = "Reply")

        val MessageId = MessageHeaderId("<msg1@example.com>")
        val InReplyTo = MessageHeaderId("<parent1@example.com>")
        val References = listOf(MessageHeaderId("<root1@example.com>"), MessageHeaderId("<root2@example.com>"))
    }
}
