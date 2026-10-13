package com.fsck.k9.backend.graph

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Date
import java.util.UUID
import net.thunderbird.core.common.mail.Flag

@Suppress("TooManyFunctions")
internal object GraphMessageMimeFactory {
    const val ATTACHMENT_ID_HEADER = "X-Thunderbird-Graph-Attachment-Id"
    const val ATTACHMENT_SIZE_HEADER = "X-Thunderbird-Graph-Attachment-Size"

    fun create(message: GraphMessage, attachments: List<GraphAttachment>): MimeMessage {
        val rawMime = createRawMime(message, attachments).toByteArray(StandardCharsets.UTF_8)
        val parsedMessage = MimeMessage.parseMimeMessage(ByteArrayInputStream(rawMime), false).apply {
            uid = message.id
            setInternalSentDate(parseDate(message.receivedDateTime ?: message.sentDateTime) ?: Date())
            setFlags(buildSet {
                if (message.isRead) add(Flag.SEEN)
                if (message.isFlagged) add(Flag.FLAGGED)
            }, true)
        }
        removeLazyAttachmentBodies(parsedMessage)
        return parsedMessage
    }

    private fun removeLazyAttachmentBodies(part: Part) {
        val multipart = part.body as? Multipart ?: return
        multipart.bodyParts.forEach { bodyPart ->
            val attachmentId = bodyPart.getHeader(ATTACHMENT_ID_HEADER).firstOrNull()
            if (attachmentId != null) {
                bodyPart.serverExtra = attachmentId
                bodyPart.body = null
            } else {
                removeLazyAttachmentBodies(bodyPart)
            }
        }
    }

    fun decodeAttachmentId(encoded: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun createRawMime(message: GraphMessage, attachments: List<GraphAttachment>): String = buildString {
        appendMessageHeaders(message)
        val body = message.bodyContent ?: message.bodyPreview.orEmpty()
        val bodyType = if (message.bodyContentType.equals("html", ignoreCase = true)) {
            "text/html"
        } else {
            "text/plain"
        }
        appendMessageBody(body, bodyType, attachments)
    }

    private fun StringBuilder.appendMessageHeaders(message: GraphMessage) {
        appendHeader("MIME-Version", "1.0")
        message.internetMessageId?.let { appendHeader("Message-ID", cleanHeader(it)) }
        parseDate(message.sentDateTime ?: message.receivedDateTime)?.let { date ->
            val formattedDate = DateTimeFormatter.RFC_1123_DATE_TIME
                .format(date.toInstant().atZone(ZoneOffset.UTC))
            appendHeader("Date", formattedDate)
        }
        (message.from ?: message.sender)?.let(::renderAddress)?.let { appendHeader("From", it) }
        message.toRecipients.renderAddresses().takeIf(String::isNotBlank)?.let { appendHeader("To", it) }
        message.ccRecipients.renderAddresses().takeIf(String::isNotBlank)?.let { appendHeader("Cc", it) }
        message.bccRecipients.renderAddresses().takeIf(String::isNotBlank)?.let { appendHeader("Bcc", it) }
        message.replyTo.renderAddresses().takeIf(String::isNotBlank)?.let { appendHeader("Reply-To", it) }
        message.subject?.let { appendHeader("Subject", encodeHeader(it)) }
    }

    private fun StringBuilder.appendMessageBody(
        body: String,
        bodyType: String,
        attachments: List<GraphAttachment>,
    ) {
        if (attachments.isEmpty()) {
            appendSinglePartBody(body, bodyType)
        } else {
            appendMultipartBody(body, bodyType, attachments)
        }
    }

    private fun StringBuilder.appendSinglePartBody(body: String, bodyType: String) {
        appendHeader("Content-Type", "$bodyType; charset=utf-8")
        appendHeader("Content-Transfer-Encoding", "base64")
        append("\r\n")
        appendBase64(body)
    }

    private fun StringBuilder.appendMultipartBody(
        body: String,
        bodyType: String,
        attachments: List<GraphAttachment>,
    ) {
        val boundary = "thunderbird-graph-${UUID.randomUUID().toString().replace("-", "")}"
        appendHeader("Content-Type", "multipart/mixed; boundary=\"$boundary\"")
        append("\r\n--$boundary\r\n")
        appendHeader("Content-Type", "$bodyType; charset=utf-8")
        appendHeader("Content-Transfer-Encoding", "base64")
        append("\r\n")
        appendBase64(body)
        attachments.forEach { attachment -> appendAttachment(attachment, boundary) }
        append("--$boundary--\r\n")
    }

    private fun StringBuilder.appendAttachment(attachment: GraphAttachment, boundary: String) {
        // Reference attachments point to remote cloud content and have no raw /$value representation.
        if (attachment.type?.endsWith("referenceAttachment", ignoreCase = true) == true) return

        append("--$boundary\r\n")
        appendHeader("Content-Type", safeContentType(attachment.contentType))
        val filename = attachment.name.orEmpty()
        val disposition = if (attachment.isInline) "inline" else "attachment"
        appendHeader("Content-Disposition", buildString {
            append(disposition)
            if (filename.isNotBlank()) append("; filename*=UTF-8''${encodeRfc2231(filename)}")
            attachment.size?.let { append("; size=").append(it) }
        })
        if (attachment.isInline && !attachment.contentId.isNullOrBlank()) {
            val contentId = attachment.contentId.removePrefix("<").removeSuffix(">")
            appendHeader("Content-ID", "<${cleanHeader(contentId)}>")
        }
        val encodedAttachmentId = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(attachment.id.toByteArray(StandardCharsets.UTF_8))
        appendHeader(ATTACHMENT_ID_HEADER, encodedAttachmentId)
        attachment.size?.let { appendHeader(ATTACHMENT_SIZE_HEADER, it.toString()) }
        appendHeader("Content-Transfer-Encoding", "binary")
        append("\r\n\r\n")
    }

    private fun StringBuilder.appendHeader(name: String, value: String) {
        append(name)
        append(": ")
        append(value)
        append("\r\n")
    }

    private fun StringBuilder.appendBase64(value: String) {
        val encoded = Base64.getMimeEncoder(76, "\r\n".toByteArray(StandardCharsets.US_ASCII))
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        append(encoded)
        append("\r\n")
    }

    private fun List<GraphEmailAddress>.renderAddresses(): String = mapNotNull(::renderAddress)
        .joinToString(", ")

    private fun renderAddress(address: GraphEmailAddress?): String? {
        val email = address?.address?.let(::cleanHeader)?.takeIf(String::isNotBlank)
        val name = address?.name?.let(::cleanHeader)?.takeIf(String::isNotBlank)
        return when {
            email.isNullOrBlank() -> null
            name.isNullOrBlank() -> email
            else -> Address(address = email, personal = name, parse = false).toEncodedString()
        }
    }

    private fun encodeHeader(value: String): String {
        val sanitized = cleanHeader(value)
        return if (sanitized.all { it.code in ASCII_PRINTABLE_MIN..ASCII_PRINTABLE_MAX }) {
            sanitized
        } else {
            val encoded = Base64.getEncoder().encodeToString(sanitized.toByteArray(StandardCharsets.UTF_8))
            "=?UTF-8?B?$encoded?="
        }
    }

    private fun cleanHeader(value: String): String = value.replace('\r', ' ').replace('\n', ' ').trim()

    private fun safeContentType(value: String?): String {
        val candidate = value?.trim().orEmpty()
        return if (CONTENT_TYPE_REGEX.matches(candidate)) candidate else "application/octet-stream"
    }

    private fun encodeRfc2231(value: String): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$&+-.^_`|~"
        return buildString {
            value.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
                val char = byte.toInt() and BYTE_MASK
                if (char.toChar() in allowed) {
                    append(char.toChar())
                } else {
                    append('%')
                    append(HEX[(char shr HEX_RADIX_SHIFT) and HEX_DIGIT_MASK])
                    append(HEX[char and HEX_DIGIT_MASK])
                }
            }
        }
    }

    private fun parseDate(value: String?): Date? = runCatching {
        value?.let { Date.from(Instant.parse(it)) }
    }.getOrNull()

    private const val ASCII_PRINTABLE_MIN = 0x20
    private const val ASCII_PRINTABLE_MAX = 0x7E
    private const val BYTE_MASK = 0xFF
    private const val HEX_DIGIT_MASK = 0x0F
    private const val HEX_RADIX_SHIFT = 4
    private val CONTENT_TYPE_REGEX = Regex("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")
    private const val HEX = "0123456789ABCDEF"
}
