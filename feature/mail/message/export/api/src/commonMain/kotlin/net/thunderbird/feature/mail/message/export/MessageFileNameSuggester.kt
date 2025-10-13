package net.thunderbird.feature.mail.message.export

import kotlinx.datetime.LocalDateTime

/**
 * Responsible for suggesting filesystem-friendly file names for exported messages.
 */
interface MessageFileNameSuggester {
    /**
     * Suggest a filename using a subject and (local) date & time, plus a required file extension.
     *
     * @param subject Subject to include in the file name. Callers should pass a reasonable default if the message has no subject.
     * @param sentDateTime (local) date & time to include as a prefix in the file name.
     * @param extension File extension without the leading dot, e.g., "eml".
     */
    fun suggestFileName(subject: String, sentDateTime: LocalDateTime, extension: String): String
}
