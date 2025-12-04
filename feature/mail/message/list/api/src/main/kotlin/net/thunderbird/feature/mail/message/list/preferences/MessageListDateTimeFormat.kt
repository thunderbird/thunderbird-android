package net.thunderbird.feature.mail.message.list.preferences

/**
 * Defines how the date and time of a message should be displayed in the message list.
 */
enum class MessageListDateTimeFormat {
    /**
     * Automatically determine the date/time format based on the available space.
     *
     * This will typically show a relative time (e.g., "5 minutes ago", "Yesterday") for recent messages
     * and an absolute date (e.g., "Dec 25") for older messages. The exact format may adapt to the
     * screen width or layout constraints.
     */
    Auto,

    /**
     * Always show the full date and time.
     */
    Full,
}
