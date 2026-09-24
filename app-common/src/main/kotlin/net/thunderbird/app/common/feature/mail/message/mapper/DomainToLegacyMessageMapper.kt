package net.thunderbird.app.common.feature.mail.message.mapper

import android.content.Context
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.internet.AddressHeaderBuilder
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import java.util.Date
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.LOG_ID
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.RecipientHeadersMap
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageAddress
import net.thunderbird.feature.mail.message.MessageBody
import net.thunderbird.feature.mail.message.MessageEnvelope
import net.thunderbird.feature.mail.message.MessageFlag
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MimeType
import com.fsck.k9.mail.Message as LegacyMessage

internal class DomainToLegacyMessageMapper(
    private val logger: Logger,
    private val context: Context,
    private val attachmentResolver: AttachmentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun map(domain: Message): LegacyMessage = withContext(ioDispatcher) {
        logger.verbose {
            """$LOG_ID parsing domain message to legacy message.
                |   domain = $domain
            """.trimMargin()
        }
        val message = MimeMessage.create()
        message.uid = domain.serverId?.value
        message.setAccountUuid(domain.accountId.toString())
        message.internalDate = domain.receivedAt.toLegacyDate()
        message.applyEnvelope(domain.envelope)
        message.applyHeaders(domain.headers)
        message.applyFlags(domain.flags)
        message.applyBody(domain)
        message
    }

    private fun MimeMessage.applyEnvelope(envelope: MessageEnvelope) {
        envelope.subject?.let(::setSubject)
        envelope.from.firstOrNull()?.toLegacyAddress()?.let(::setFrom)
        envelope.sender?.toLegacyAddress()?.let(::setSender)
        replyTo = envelope.replyTo.map { it.toLegacyAddress() }.toTypedArray()
        setLegacyRecipients(
            headerName = RecipientHeadersMap.getValue(LegacyMessage.RecipientType.TO),
            addresses = envelope.to,
        )
        setLegacyRecipients(
            headerName = RecipientHeadersMap.getValue(LegacyMessage.RecipientType.CC),
            addresses = envelope.cc,
        )
        setLegacyRecipients(
            headerName = RecipientHeadersMap.getValue(LegacyMessage.RecipientType.BCC),
            addresses = envelope.bcc,
        )
        setSentDate(envelope.sentAt.toLegacyDate(), false)
    }

    private fun MimeMessage.setLegacyRecipients(headerName: String, addresses: List<MessageAddress>) {
        if (addresses.isEmpty()) return
        val legacyAddresses = addresses.map { it.toLegacyAddress() }.toTypedArray()
        setHeader(headerName, AddressHeaderBuilder.createHeaderValue(legacyAddresses))
    }

    private fun MimeMessage.applyHeaders(headers: MessageHeaders) {
        setMessageId(headers.messageId.value)
        headers.inReplyTo.takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { it.value }
            ?.let(::setInReplyTo)
        headers.references?.takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { it.value }
            ?.let(::setReferences)
        headers.extra.forEach { (key, value) -> addHeader(key, value) }
    }

    private fun MimeMessage.applyFlags(flags: Set<MessageFlag>) {
        flags.forEach { flag -> flag.toFlagOrNull()?.let { setFlag(it, true) } }
    }

    private fun MessageFlag.toFlagOrNull(): Flag? = when (this) {
        MessageFlag.Read -> Flag.SEEN

        MessageFlag.Starred -> Flag.FLAGGED

        MessageFlag.Answered -> Flag.ANSWERED

        MessageFlag.Forwarded -> Flag.FORWARDED

        MessageFlag.Draft -> Flag.DRAFT

        MessageFlag.Deleted -> Flag.DELETED

        is MessageFlag.CustomFlag if this.value == "X_DESTROYED" -> Flag.X_DESTROYED

        is MessageFlag.CustomFlag if this.value == "X_SEND_FAILED" -> Flag.X_SEND_FAILED

        is MessageFlag.CustomFlag if this.value == "X_SEND_IN_PROGRESS" -> Flag.X_SEND_IN_PROGRESS

        is MessageFlag.CustomFlag if this.value == "X_REMOTE_COPY_STARTED" -> Flag.X_REMOTE_COPY_STARTED

        is MessageFlag.CustomFlag if this.value == "X_MIGRATED_FROM_V50" -> Flag.X_MIGRATED_FROM_V50

        is MessageFlag.CustomFlag if this.value == "X_DRAFT_OPENPGP_INLINE" -> Flag.X_DRAFT_OPENPGP_INLINE

        is MessageFlag.CustomFlag if this.value == "X_SUBJECT_DECRYPTED" -> Flag.X_SUBJECT_DECRYPTED

        // The legacy Flag enum is closed and has no Junk/another custom-keyword equivalent.
        else -> null
    }

    private fun MimeMessage.applyBody(domain: Message) {
        val attachments = domain.attachments.orEmpty()
        val textBody = domain.body?.toAlternativeMultipart()

        when {
            textBody == null && attachments.isEmpty() -> Unit

            attachments.isEmpty() -> MimeMessageHelper.setBody(this, textBody)

            textBody == null -> {
                val mixed = MimeMultipart.newInstance()
                with(attachmentResolver) {
                    attachments.forEach { mixed.addBodyPart(it.toBodyPart(context)) }
                }
                MimeMessageHelper.setBody(this, mixed)
            }

            else -> {
                val mixed = MimeMultipart.newInstance()
                mixed.addBodyPart(MimeBodyPart.create(textBody))
                with(attachmentResolver) {
                    attachments.forEach { mixed.addBodyPart(it.toBodyPart(context)) }
                }
                MimeMessageHelper.setBody(this, mixed)
            }
        }
    }

    private fun MessageBody.toAlternativeMultipart(): MimeMultipart {
        val boundary = BoundaryGenerator.getInstance().generateBoundary()
        return MimeMultipart(boundary).apply {
            setSubType("alternative")
            addBodyPart(MimeBodyPart.create(TextBody(plainText), MimeType.TextPlain.value))
            addBodyPart(MimeBodyPart.create(TextBody(html), MimeType.TextHtml.value))
        }
    }

    private fun MessageAddress.toLegacyAddress(): Address {
        val personal = label.takeIf { it != value }
        return Address(value, personal, false)
    }

    private fun LocalDateTime.toLegacyDate(): Date =
        Date(toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
}
