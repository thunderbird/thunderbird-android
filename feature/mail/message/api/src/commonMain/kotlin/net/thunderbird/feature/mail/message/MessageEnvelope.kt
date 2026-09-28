package net.thunderbird.feature.mail.message

import kotlinx.datetime.LocalDateTime
import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * Header fields describing a message's sender, recipients, and subject.
 * Matches the data an IMAP ENVELOPE fetch returns.
 *
 * @property subject Message subject.
 * @property from Author(s) of the message.
 * @property sender Address that actually sent the message, if different from [from].
 * @property replyTo Address replies should be sent to, if different from [from].
 * @property to Primary recipients.
 * @property cc Carbon-copy recipients.
 * @property bcc Blind carbon-copy recipients.
 * @property sentAt Time the message was sent, as set by the sender.
 */
@PiiSafe.HasPii
data class MessageEnvelope(
    val subject: String?,
    val from: List<MessageAddress>,
    val sender: MessageAddress?,
    val replyTo: List<MessageAddress>,
    val to: List<MessageAddress>,
    val cc: List<MessageAddress>,
    val bcc: List<MessageAddress>,
    val sentAt: LocalDateTime,
)
