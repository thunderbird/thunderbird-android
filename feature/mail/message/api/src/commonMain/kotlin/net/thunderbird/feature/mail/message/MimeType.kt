package net.thunderbird.feature.mail.message

import net.thunderbird.feature.mail.message.MimeType.Companion.ApplicationOctetStream

/**
 * Represents a MIME media type used to identify content types in email messages and attachments.
 *
 * This sealed interface ensures only this module can define new [MimeType] types.
 * Common MIME types are provided as predefined constants in the companion object.
 */
sealed interface MimeType {
    val value: String

    companion object {
        /**
         * MIME type constant for plain text content (`text/plain`).
         *
         * Used to identify plain text message bodies and attachments without formatting or markup.
         */
        val TextPlain: MimeType = NamedMimeType("text/plain")

        /**
         * MIME type constant for HTML content (`text/html`).
         *
         * Used to identify HTML message bodies and attachments that contain formatted markup.
         */
        val TextHtml: MimeType = NamedMimeType("text/html")

        /**
         * MIME type constant for generic binary data (`application/octet-stream`).
         *
         * Used to identify arbitrary binary attachments or content with no specific or unknown type.
         * This is the default MIME type for unrecognized binary data.
         */
        val ApplicationOctetStream: MimeType = NamedMimeType("application/octet-stream")

        /**
         * MIME type constant for K-9 Mail settings export files (`application/x-k9settings`).
         *
         * Used to identify attachments or files containing exported K-9 Mail configuration data.
         */
        val K9Settings: MimeType = NamedMimeType("application/x-k9settings")

        /**
         * Default MIME type used for attachments when the actual content type cannot be determined.
         *
         * This is an alias for [ApplicationOctetStream] (`application/octet-stream`), representing
         * arbitrary binary data.
         */
        val AttachmentDefault: MimeType = ApplicationOctetStream
    }
}

/**
 * Creates a MimeType instance from the given string value.
 *
 * @param value The MIME type string (e.g., "text/plain", "application/pdf")
 * @return A MimeType instance representing the specified content type
 */
fun MimeType(value: String): MimeType = NamedMimeType(value)

/**
 * Private implementation of MimeType that wraps a MIME type string value.
 */
@JvmInline
private value class NamedMimeType(override val value: String) : MimeType {
    override fun toString(): String = value
}
