package net.thunderbird.app.common.feature.mail.message.mapper

import android.net.Uri
import com.eygraber.uri.toKmpUri
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.LocalMimeMessage
import com.fsck.k9.mailstore.LocalPart
import com.fsck.k9.message.html.HtmlConverter
import java.util.Date
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.app.common.feature.mail.message.domain.model.LegacyMessageSource
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.LOG_ID
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.PREVIEW_FALLBACK_LENGTH
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderId
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
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.MimeType
import net.thunderbird.feature.mail.message.ThreadId
import com.fsck.k9.mail.Message as LegacyMessage

/**
 * Maps legacy K-9 Mail [messages][LegacyMessage] to the domain [Message] model.
 *
 * Mapping extracts the envelope, headers, body and attachments from the legacy MIME structure.
 * Values only available for locally stored messages, such as database identifiers, are `null` when
 * the message isn't backed by the local store.
 *
 * @param messageIdLegacyEntityIdFactory Creates [MessageId]s from legacy database identifiers.
 * @param threadIdLegacyEntityIdFactory Creates [ThreadId]s from legacy database identifiers.
 * @param folderIdLegacyEntityIdFactory Creates [FolderId]s from legacy database identifiers.
 * @param attachmentResolver Resolves attachment metadata and content.
 * @param ioDispatcher The dispatcher used for the blocking MIME and file operations.
 */
internal class LegacyToDomainMessageMapper(
    private val logger: Logger,
    private val messageIdLegacyEntityIdFactory: LegacyEntityIdFactory<MessageId>,
    private val threadIdLegacyEntityIdFactory: LegacyEntityIdFactory<ThreadId>,
    private val folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    private val attachmentResolver: AttachmentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Maps [dto] to its domain [Message] representation.
     *
     * @throws IllegalArgumentException if the legacy message has no account UUID.
     */
    suspend fun map(dto: LegacyMessage): Message = withContext(ioDispatcher) {
        logger.verbose {
            """$LOG_ID parsing legacy message to domain message.
                |   dto = $dto
            """.trimMargin()
        }

        val localMessage = dto.asLocalMessageOrNull()
        logger.verbose { "$LOG_ID dto local message: $localMessage" }
        val body: MessageBody? = dto.toComposedMessageBodyOrNull()
        val attachments: List<MessageAttachment>? = dto.toComposedAttachmentsOrNull()

        Message(
            id = localMessage?.databaseId?.let(messageIdLegacyEntityIdFactory::of),
            serverId = dto.uid?.let(::MessageServerId),
            accountId = requireNotNull(dto.accountUuid?.let(AccountIdFactory::of)) {
                "Can't map legacy message to domain message. The Account ID is required."
            },
            folderId = localMessage?.folder?.databaseId?.let(folderIdLegacyEntityIdFactory::of),
            threadRoot = localMessage?.threadId?.let(threadIdLegacyEntityIdFactory::of),
            receivedAt = (dto.internalDate ?: Date()).toDomainLocalDateTime(),
            envelope = dto.toMessageEnvelop(),
            headers = dto.toMessageHeaders(),
            body = body,
            downloadState = dto.toMessageDownloadState(),
            flags = dto.toMessageFlags(),
            attachments = attachments,
            // LocalStore doesn't persist a message size column.
            size = null,
            source = LegacyMessageSource(dto),
        )
    }

    /**
     * Maps the subject, the participating addresses and the sent date to a [MessageEnvelope].
     */
    private fun LegacyMessage.toMessageEnvelop(): MessageEnvelope = MessageEnvelope(
        subject = subject.orEmpty(),
        from = from.map { it.toMessageAddress() },
        sender = sender.firstOrNull()?.toMessageAddress(),
        replyTo = replyTo.map { it.toMessageAddress() },
        to = getRecipients(LegacyMessage.RecipientType.TO).map { it.toMessageAddress() },
        cc = getRecipients(LegacyMessage.RecipientType.CC).map { it.toMessageAddress() },
        bcc = getRecipients(LegacyMessage.RecipientType.BCC).map { it.toMessageAddress() },
        sentAt = (sentDate ?: internalDate ?: Date()).toDomainLocalDateTime(),
    )

    /**
     * Maps the threading-related headers, `Message-ID`, `References` and `In-Reply-To`, together
     * with any header not already captured by another field on [Message] (e.g. `X-Original-To`).
     */
    private fun LegacyMessage.toMessageHeaders(): MessageHeaders {
        val inReplyToHeader = getHeader(MessageHeaders.IN_REPLY_TO_HEADER_NAME)
        return MessageHeaders(
            messageId = MessageHeaderId(messageId.orEmpty()),
            references = references.firstOrNull()
                ?.let(Utility::extractMessageIds)
                ?.map(::MessageHeaderId)
                ?: emptyList(),
            inReplyTo = inReplyToHeader.firstOrNull()
                ?.let(Utility::extractMessageIds)
                ?.map(::MessageHeaderId)
                ?: emptyList(),
            extra = headers
                .filterNot { it.name in DefaultMessageDataMapper.KnownHeaders }
                .associate { it.name to it.value },
        )
    }

    /**
     * Extracts the viewable text parts into a [MessageBody], converting between HTML and plain text
     * when only one of the representations is present.
     *
     * @return The composed body, or `null` when the message has no viewable text part.
     */
    private fun LegacyMessage.toComposedMessageBodyOrNull(): MessageBody? {
        logger.verbose { "$LOG_ID extracting legacy message body" }
        val textParts = MessageExtractor.getTextParts(this)
        logger.verbose { "$LOG_ID text parts extracted = $textParts" }
        val htmlPart = textParts.firstOrNull { MimeUtility.isSameMimeType(it.mimeType, MimeType.TextHtml.value) }
        val plainTextPart = textParts.firstOrNull { MimeUtility.isSameMimeType(it.mimeType, MimeType.TextPlain.value) }

        val plainText = plainTextPart?.let(MessageExtractor::getTextFromPart)
        logger.verbose { "$LOG_ID plain text extracted = $plainText" }
        val html = htmlPart?.let(MessageExtractor::getTextFromPart)
            ?: plainText?.let(HtmlConverter::textToHtml)
            ?: return run {
                logger.warn {
                    "$LOG_ID Couldn't find any plain text nor html part in the text parts of given message. " +
                        "Message body is null."
                }
                null
            }
        logger.verbose { "$LOG_ID html extracted = $html" }
        val preview = (plainText ?: HtmlConverter.htmlToText(html)).take(PREVIEW_FALLBACK_LENGTH)
        logger.verbose { "$LOG_ID preview extracted = $preview" }

        return MessageBody(
            preview = preview,
            html = html,
            plainText = plainText ?: HtmlConverter.htmlToText(html),
        )
    }

    /**
     * Extracts the attachment parts of the message.
     *
     * @return The mapped attachments, or `null` when the message has no attachment part.
     */
    private fun LegacyMessage.toComposedAttachmentsOrNull(): List<MessageAttachment>? {
        logger.verbose { "$LOG_ID extracting legacy message attachments" }
        val attachmentParts = ArrayList<Part>()
        logger.verbose { "$LOG_ID finding attachments within message" }
        MessageExtractor.findViewablesAndAttachments(this, null, attachmentParts)
        logger.verbose { "$LOG_ID attachment parts = $attachmentParts" }
        return attachmentParts.takeIf { it.isNotEmpty() }?.map { it.toComposedMessageAttachment() }
    }

    /**
     * Maps an attachment [Part] to either an inline or a regular [MessageAttachment].
     *
     * Copies the attachment's bytes out of its (possibly transient, e.g. TempFileBody) Body into a
     * file this class controls, so the resulting URI stays valid regardless of what backed the
     * original part.
     *
     * When the part's content hasn't been downloaded yet (`body == null`) — e.g. after a partial
     * sync that skipped attachments, or a message loaded without `FetchProfile.Item.BODY` — no
     * bytes exist to copy. This mirrors `AttachmentInfoExtractor`'s `isContentAvailable` check: the
     * resulting [MessageAttachment] gets a null [MessageAttachment.internalUri], and its size falls
     * back to the size persisted for the part (via [LocalPart]) when available.
     */
    private fun Part.toComposedMessageAttachment(): MessageAttachment {
        with(attachmentResolver) {
            logger.verbose { "$LOG_ID parsing an attachment part to message attachment" }
            val resolvedName = resolveAttachmentName()
            logger.verbose { "$LOG_ID attachment resolved name = $resolvedName" }

            val copiedFile = resolveAttachmentFile()
            val uri = copiedFile?.let { Uri.fromFile(it).toKmpUri() }
            logger.verbose { "$LOG_ID attachment uri = $uri" }

            val resolvedSize = resolveAttachmentSize(copiedFile)
            logger.verbose { "$LOG_ID attachment size = $resolvedSize" }

            return if (isInline()) {
                logger.verbose { "$LOG_ID attachment mapped to inline attachment" }
                MessageAttachment.Inline(
                    mimeType = mimeType?.let(::MimeType),
                    displayName = resolvedName?.let(::MessageAttachmentDisplayName),
                    size = resolvedSize,
                    internalUri = uri,
                    contentId = contentId?.let(::MessageAttachmentContentId),
                )
            } else {
                logger.verbose { "$LOG_ID attachment mapped to regular attachment" }
                MessageAttachment.Regular(
                    mimeType = mimeType?.let(::MimeType),
                    displayName = resolvedName?.let(::MessageAttachmentDisplayName),
                    size = resolvedSize,
                    internalUri = uri,
                )
            }
        }
    }

    /**
     * Returns the backing [LocalMessage] when the message is stored locally, `null` otherwise.
     */
    private fun LegacyMessage.asLocalMessageOrNull(): LocalMessage? = when (this) {
        is LocalMimeMessage -> message
        else -> this as? LocalMessage
    }

    /**
     * Maps a legacy [Address] to a [MessageAddress], falling back to the address itself as label
     * when no personal name is set.
     */
    private fun Address.toMessageAddress(): MessageAddress = MessageAddress(
        value = address,
        label = personal ?: address,
    )

    /**
     * Converts a legacy [Date] to a [LocalDateTime] in the current system time zone.
     */
    private fun Date.toDomainLocalDateTime(): LocalDateTime =
        Instant.fromEpochMilliseconds(time).toLocalDateTime(TimeZone.currentSystemDefault())

    /**
     * Derives the [MessageDownloadState] from the legacy download flags.
     */
    private fun LegacyMessage.toMessageDownloadState(): MessageDownloadState = when {
        isSet(Flag.X_DOWNLOADED_FULL) -> MessageDownloadState.FULL
        isSet(Flag.X_DOWNLOADED_PARTIAL) -> MessageDownloadState.PARTIAL
        else -> MessageDownloadState.ENVELOPE
    }

    /**
     * Maps the legacy [Flag]s that have a domain counterpart to [MessageFlag]s.
     */
    private fun LegacyMessage.toMessageFlags(): Set<MessageFlag> = buildSet {
        if (isSet(Flag.SEEN)) add(MessageFlag.Read)
        if (isSet(Flag.FLAGGED)) add(MessageFlag.Starred)
        if (isSet(Flag.ANSWERED)) add(MessageFlag.Answered)
        if (isSet(Flag.FORWARDED)) add(MessageFlag.Forwarded)
        if (isSet(Flag.DRAFT)) add(MessageFlag.Draft)
        if (isSet(Flag.DELETED)) add(MessageFlag.Deleted)
        if (isSet(Flag.X_DESTROYED)) add(MessageFlag.CustomFlag(Flag.X_DESTROYED.name))
        if (isSet(Flag.X_SEND_FAILED)) add(MessageFlag.CustomFlag(Flag.X_SEND_FAILED.name))
        if (isSet(Flag.X_SEND_IN_PROGRESS)) add(MessageFlag.CustomFlag(Flag.X_SEND_IN_PROGRESS.name))
        if (isSet(Flag.X_REMOTE_COPY_STARTED)) add(MessageFlag.CustomFlag(Flag.X_REMOTE_COPY_STARTED.name))
        if (isSet(Flag.X_MIGRATED_FROM_V50)) add(MessageFlag.CustomFlag(Flag.X_MIGRATED_FROM_V50.name))
        if (isSet(Flag.X_DRAFT_OPENPGP_INLINE)) add(MessageFlag.CustomFlag(Flag.X_DRAFT_OPENPGP_INLINE.name))
        if (isSet(Flag.X_SUBJECT_DECRYPTED)) add(MessageFlag.CustomFlag(Flag.X_SUBJECT_DECRYPTED.name))
    }
}
