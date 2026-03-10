package net.thunderbird.feature.mail.message.list.ui.state

/**
 * Represents an attachment in a message item.
 *
 * @property filename The name of the attached file.
 * @property url The URL to download the attachment.
 * @property size The size of the attachment in a human-readable format (e.g., KB, MB).
 */
data class MessageItemAttachment(
    val filename: String,
    val url: String,
    val size: Float,
)
