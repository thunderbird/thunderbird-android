package net.thunderbird.feature.mail.message
/**
 * Value of an attachment's Content-ID header, used to resolve "cid:" references in HTML bodies.
 *
 * @property value The header value.
 */
@JvmInline
value class MessageAttachmentContentId(val value: String) {
    companion object {
        private const val CONTENT_ID_PREFIX = "cid:"
    }

    /** Returns this id formatted as a "cid:" URI, as used in HTML "src" attributes. */
    fun toHtmlId(): String = if (value.startsWith(CONTENT_ID_PREFIX)) {
        value
    } else {
        "$CONTENT_ID_PREFIX$value"
    }
}
