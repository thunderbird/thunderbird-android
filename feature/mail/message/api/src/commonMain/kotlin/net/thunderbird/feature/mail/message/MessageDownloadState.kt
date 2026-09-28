package net.thunderbird.feature.mail.message

/**
 * How much of a message has been downloaded from the server.
 */
enum class MessageDownloadState {
    /** Only the envelope (subject, addresses, dates, threading headers) is available. */
    ENVELOPE,

    /** Part of the body was downloaded, e.g. up to a size limit. */
    PARTIAL,

    /** The whole message was downloaded. */
    FULL,
}
