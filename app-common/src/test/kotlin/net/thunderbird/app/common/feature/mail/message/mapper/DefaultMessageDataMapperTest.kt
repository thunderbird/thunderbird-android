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
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.LegacyThreadIdFactory
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageAddress
import net.thunderbird.feature.mail.message.MessageBody
import net.thunderbird.feature.mail.message.MessageDownloadState
import net.thunderbird.feature.mail.message.MessageEnvelope
import net.thunderbird.feature.mail.message.MessageFlag
import net.thunderbird.feature.mail.message.MessageHeaderId
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.robolectric.RuntimeEnvironment
import com.fsck.k9.mail.Message as LegacyMessage

class DefaultMessageDataMapperTest : RobolectricTest() {

    private val testLogger = TestLogger()
    private val context = RuntimeEnvironment.getApplication()
    private val testSubject: MessageDataMapper<LegacyMessage> = DefaultMessageDataMapper(
        logger = testLogger,
        messageIdLegacyEntityIdFactory = LegacyMessageIdFactory,
        threadIdLegacyEntityIdFactory = LegacyThreadIdFactory,
        folderIdLegacyEntityIdFactory = LegacyFolderIdFactory,
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
            prop(LegacyMessage::getAccountUuid).isEqualTo(ACCOUNT_ID)
            prop(LegacyMessage::getUid).isEqualTo(SERVER_ID)
            prop(LegacyMessage::getInternalDate).transform { it.toLocalDateTime() }.isEqualTo(RECEIVED_AT)
            prop(LegacyMessage::getSubject).isEqualTo(SUBJECT)
            prop(LegacyMessage::getFrom).isEqualTo(arrayOf(FROM.toLegacyAddress()))
            prop(LegacyMessage::getSender).isEmpty()
            prop(LegacyMessage::getReplyTo).isEqualTo(arrayOf(REPLY_TO.toLegacyAddress()))
            transform { it.getRecipients(LegacyMessage.RecipientType.TO) }
                .isEqualTo(arrayOf(TO.toLegacyAddress()))
            transform { it.getRecipients(LegacyMessage.RecipientType.CC) }.isEmpty()
            transform { it.getRecipients(LegacyMessage.RecipientType.BCC) }.isEmpty()
            prop(LegacyMessage::getSentDate).transform { it.toLocalDateTime() }.isEqualTo(SENT_AT)
            prop(LegacyMessage::getMessageId).isEqualTo(MESSAGE_ID.value)
            transform { it.getHeader("In-Reply-To") }.isEqualTo(arrayOf(IN_REPLY_TO.value))
            prop(LegacyMessage::getReferences).isEqualTo(arrayOf(REFERENCES.joinToString(" ") { it.value }))
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

    private fun buildDomainMessage(
        downloadState: MessageDownloadState = MessageDownloadState.FULL,
    ): Message = Message(
        id = null,
        serverId = MessageServerId(SERVER_ID),
        accountId = AccountIdFactory.of(ACCOUNT_ID),
        folderId = null,
        threadRoot = null,
        receivedAt = RECEIVED_AT,
        envelope = MessageEnvelope(
            subject = SUBJECT,
            from = listOf(FROM),
            sender = null,
            replyTo = listOf(REPLY_TO),
            to = listOf(TO),
            cc = emptyList(),
            bcc = emptyList(),
            sentAt = SENT_AT,
        ),
        headers = MessageHeaders(
            messageId = MESSAGE_ID,
            references = REFERENCES,
            inReplyTo = listOf(IN_REPLY_TO),
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
        message.setAccountUuid(ACCOUNT_ID)
        message.uid = SERVER_ID
        message.internalDate = RECEIVED_AT.toLegacyDate()
        message.setSubject(SUBJECT)
        message.setFrom(FROM.toLegacyAddress())
        message.replyTo = arrayOf(REPLY_TO.toLegacyAddress())
        message.setHeader("To", AddressHeaderBuilder.createHeaderValue(arrayOf(TO.toLegacyAddress())))
        message.setSentDate(SENT_AT.toLegacyDate(), false)
        message.setMessageId(MESSAGE_ID.value)
        message.setInReplyTo(IN_REPLY_TO.value)
        message.setReferences(REFERENCES.joinToString(" ") { it.value })
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
        const val ACCOUNT_ID = "11111111-1111-1111-1111-111111111111"
        const val SERVER_ID = "100"
        const val SUBJECT = "Test subject"
        const val PLAIN_TEXT = "Hello, this is a test message body."
        const val HTML = "<p>Hello, this is a test message body.</p>"

        val RECEIVED_AT = LocalDateTime(2024, 1, 15, 10, 30, 0)
        val SENT_AT = LocalDateTime(2024, 1, 15, 10, 25, 0)

        val FROM = MessageAddress(value = "alice@example.com", label = "Alice")
        val TO = MessageAddress(value = "bob@example.com", label = "Bob")
        val REPLY_TO = MessageAddress(value = "reply@example.com", label = "Reply")

        val MESSAGE_ID = MessageHeaderId("<msg1@example.com>")
        val IN_REPLY_TO = MessageHeaderId("<parent1@example.com>")
        val REFERENCES = listOf(MessageHeaderId("<root1@example.com>"), MessageHeaderId("<root2@example.com>"))

        const val EXTRA_HEADER_NAME = "X-Original-To"
        const val EXTRA_HEADER_VALUE = "someone@example.com"
    }
}
