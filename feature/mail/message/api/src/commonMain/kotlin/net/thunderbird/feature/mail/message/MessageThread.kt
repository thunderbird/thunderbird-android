package net.thunderbird.feature.mail.message

import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * A conversation: a root message and the other messages in the same thread.
 *
 * @property root First message in the thread.
 * @property children The other messages in the thread, in chronological order.
 */
@PiiSafe.HasPii
data class MessageThread(
    val root: Message,
    val children: List<Message>,
)
