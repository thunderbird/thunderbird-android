package net.thunderbird.feature.mail.message.composer.signature

/**
 * Sanitizes user-supplied HTML signatures before they are inserted into outgoing mail.
 */
fun interface HtmlSignatureSanitizer {
    /**
     * Sanitizes user-supplied HTML signature content to ensure it is safe for display and use in outgoing mail.
     *
     * @param html the raw HTML signature content to sanitize
     * @return the sanitized HTML signature that is safe to display and include in messages
     */
    fun sanitize(html: String): String
}
