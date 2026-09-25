package net.thunderbird.app.common.feature.mail.message.mapper

import android.content.Context
import com.fsck.k9.mail.internet.MimeHeader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageBody
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.ThreadId
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import com.fsck.k9.mail.Message as LegacyMessage

internal class DefaultMessageDataMapper(
    logger: Logger,
    messageIdLegacyEntityIdFactory: LegacyEntityIdFactory<MessageId>,
    threadIdLegacyEntityIdFactory: LegacyEntityIdFactory<ThreadId>,
    folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    attachmentResolver: AttachmentResolver,
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessageDataMapper<LegacyMessage> {
    private val legacyToDomainMessageMapper = LegacyToDomainMessageMapper(
        logger = logger,
        messageIdLegacyEntityIdFactory = messageIdLegacyEntityIdFactory,
        threadIdLegacyEntityIdFactory = threadIdLegacyEntityIdFactory,
        folderIdLegacyEntityIdFactory = folderIdLegacyEntityIdFactory,
        attachmentResolver = attachmentResolver,
        ioDispatcher = ioDispatcher,
    )
    private val domainToLegacyMessageMapper = DomainToLegacyMessageMapper(
        logger = logger,
        context = context,
        attachmentResolver = attachmentResolver,
        ioDispatcher = ioDispatcher,
    )

    override suspend fun toDomain(dto: LegacyMessage): Message = legacyToDomainMessageMapper.map(dto)

    override suspend fun toDto(domain: Message): LegacyMessage = domainToLegacyMessageMapper.map(domain)

    internal companion object {
        const val LOG_ID = "[mapper][message <-> legacy message]"
        const val PREVIEW_FALLBACK_LENGTH = 140
        val DefaultRecipientHeaders = mapOf(
            LegacyMessage.RecipientType.TO to "To",
            LegacyMessage.RecipientType.CC to "CC",
            LegacyMessage.RecipientType.BCC to "BCC",
        )
        private val ExtraRecipientHeaders = mapOf(
            LegacyMessage.RecipientType.X_ORIGINAL_TO to "X-Original-To",
            LegacyMessage.RecipientType.DELIVERED_TO to "Delivered-To",
            LegacyMessage.RecipientType.X_ENVELOPE_TO to "X-Envelope-To",
        )
        val RecipientHeadersMap = DefaultRecipientHeaders + ExtraRecipientHeaders

        /**
         * Header names already represented by another field on the domain [Message] (envelope fields,
         * threading headers), or reconstructed from [MessageBody] when writing the message back
         * ([MimeHeader.HEADER_CONTENT_TYPE] and friends, plus `MIME-Version`, set by
         * [com.fsck.k9.mail.internet.MimeMessageHelper] itself).
         *
         * Excluded from [MessageHeaders.extra] so it only holds headers with no other home, such as
         * `X-Original-To`, instead of duplicating headers we already store elsewhere.
         */
        val KnownHeaders = setOf(
            MimeHeader.SUBJECT,
            "From",
            "Sender",
            "Reply-to",
            "Date",
            "Message-ID",
            "In-Reply-To",
            "References",
            "MIME-Version",
            MimeHeader.HEADER_CONTENT_TYPE,
            MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING,
            MimeHeader.HEADER_CONTENT_DISPOSITION,
            MimeHeader.HEADER_CONTENT_ID,
        ) + DefaultRecipientHeaders.values
    }
}
