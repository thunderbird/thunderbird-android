package net.thunderbird.feature.mail.message

/**
 * Value of an RFC 5322 Message-ID header, e.g. "<abc123@example.com>".
 * Globally unique and set by the client that sent the message.
 *
 * @property value The header value.
 */
@JvmInline
value class MessageHeaderId(val value: String)
