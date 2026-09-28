package net.thunderbird.feature.mail.message

/**
 * The content of a message, in both renderable forms.
 *
 * @property preview Short excerpt shown in message lists.
 * @property html HTML content to render.
 * @property plainText Plain text content to render.
 */
data class MessageBody(
    val preview: String,
    val html: String,
    val plainText: String,
)
