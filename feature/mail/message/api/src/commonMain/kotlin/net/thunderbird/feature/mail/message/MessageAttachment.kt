package net.thunderbird.feature.mail.message

import com.eygraber.uri.Uri
import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * A file attached to a message.
 *
 * @property mimeType MIME type of the attachment content, if known.
 * @property displayName File name shown to the user, if known.
 * @property size Size of the attachment content, in bytes.
 * @property internalUri Location of the downloaded attachment content in local storage, or null if
 * the content hasn't been downloaded yet.
 */
sealed interface MessageAttachment {
    val mimeType: MimeType?
    val displayName: MessageAttachmentDisplayName?
    val size: Long
    val internalUri: Uri?

    /**
     * Converts this attachment to its `Content-Disposition` string representation.
     *
     * @return The Content-Disposition value for this attachment
     */
    fun toDisposition(): String

    /**
     * An attachment referenced from the message body, e.g. an image shown inline in HTML.
     *
     * @property mimeType MIME type of the attachment content, if known.
     * @property displayName File name shown to the user, if known.
     * @property size Size of the attachment content, in bytes.
     * @property internalUri Location of the downloaded attachment content in local storage, or null
     * if the content hasn't been downloaded yet.
     * @property contentId Content-ID used to reference this attachment from the HTML body, if any.
     */
    @PiiSafe.HasPii
    data class Inline(
        override val mimeType: MimeType?,
        @get:PiiSafe.Mask
        override val displayName: MessageAttachmentDisplayName?,
        override val size: Long,
        override val internalUri: Uri?,
        @get:PiiSafe.Mask
        val contentId: MessageAttachmentContentId?,
    ) : MessageAttachment {
        override fun toDisposition(): String = "inline"
    }

    /**
     * An attachment not referenced from the message body.
     *
     * @property mimeType MIME type of the attachment content, if known.
     * @property displayName File name shown to the user, if known.
     * @property size Size of the attachment content, in bytes.
     * @property internalUri Location of the downloaded attachment content in local storage, or null
     * if the content hasn't been downloaded yet.
     */
    @PiiSafe.HasPii
    data class Regular(
        override val mimeType: MimeType?,
        @get:PiiSafe.Mask
        override val displayName: MessageAttachmentDisplayName?,
        override val size: Long,
        override val internalUri: Uri?,
    ) : MessageAttachment {
        override fun toDisposition(): String = "attachment"
    }
}
