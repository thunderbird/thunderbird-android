package net.thunderbird.feature.mail.message

/**
 * A flag or keyword applied to a message, e.g. an IMAP system or custom flag.
 */
sealed interface MessageFlag {
    /** Message has been read. */
    data object Read : MessageFlag

    /** Message has been starred/flagged by the user. */
    data object Starred : MessageFlag

    /** Message has been replied to. */
    data object Answered : MessageFlag

    /** Message has been forwarded. */
    data object Forwarded : MessageFlag

    /** Message is a draft. */
    data object Draft : MessageFlag

    /** Message is marked for deletion, pending removal from the server. */
    data object Deleted : MessageFlag

    /** Message is marked as spam/junk. */
    data object Junk : MessageFlag

    /** Any other flag or keyword not covered above. */
    data class CustomFlag(val value: String) : MessageFlag
}
