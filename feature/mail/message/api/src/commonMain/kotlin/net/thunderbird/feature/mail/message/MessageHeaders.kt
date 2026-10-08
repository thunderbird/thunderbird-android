package net.thunderbird.feature.mail.message

import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * Threading headers defined by RFC 5322.
 *
 * @property messageId This message's own Message-ID.
 * @property references Message-IDs of the whole thread, in order. Null if not fetched yet.
 * @property inReplyTo Message-IDs of the message(s) this one replies to.
 */
@PiiSafe.HasPii
data class MessageHeaders(
    @get:PiiSafe.Mask
    val messageId: MessageHeaderId,
    @get:PiiSafe.Mask
    val references: List<MessageHeaderId>?,
    @get:PiiSafe.Mask
    val inReplyTo: List<MessageHeaderId>,
    val extra: Map<String, String>,
) {
    companion object {
        const val IN_REPLY_TO_HEADER_NAME = "In-Reply-To"
    }
}
