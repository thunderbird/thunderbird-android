package net.thunderbird.feature.mail.message

/**
 * Identifier assigned to a message by the mail server, e.g. an IMAP UID.
 * Can change if the message is moved or the server re-assigns it.
 *
 * @property value The identifier value.
 */
@JvmInline
value class MessageServerId(val value: String)
