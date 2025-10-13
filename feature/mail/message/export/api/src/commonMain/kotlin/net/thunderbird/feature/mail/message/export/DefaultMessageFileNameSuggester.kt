package net.thunderbird.feature.mail.message.export

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

/**
 * Default, format-agnostic implementation for [MessageFileNameSuggester].
 */
class DefaultMessageFileNameSuggester : MessageFileNameSuggester {
    override fun suggestFileName(subject: String, sentDateTime: LocalDateTime, extension: String): String {
        val normalizedSubject = subject.trim()
            .lowercase()
            .replace(nonAlphanumericRegex, "-")
            .replace(consecutiveDashesRegex, "-")
            .trim('-')
            .ifEmpty { "message" }

        return "${fileNameDateFormat.format(sentDateTime)}_$normalizedSubject.$extension"
    }

    private companion object {
        val fileNameDateFormat = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char('_')
            hour()
            char('-')
            minute()
        }

        val nonAlphanumericRegex = Regex("[^a-z0-9-]+")
        val consecutiveDashesRegex = Regex("-+")
    }
}
