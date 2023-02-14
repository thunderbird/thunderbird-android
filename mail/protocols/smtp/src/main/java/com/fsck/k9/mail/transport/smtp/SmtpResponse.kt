package com.fsck.k9.mail.transport.smtp

internal data class SmtpResponse(
    val replyCode: Int,
    val enhancedStatusCode: EnhancedStatusCode?,
    val texts: List<String>,
) {
    val isNegativeResponse = replyCode >= 400

    val joinedText: String
        get() = texts.joinToString(separator = " ")

    fun toLogString(omitText: Boolean, linePrefix: String): String {
        return buildString {
            if (omitText) {
                append(linePrefix)
                append(replyCode)
                appendIfNotNull(enhancedStatusCode, prefix = ' ')
                if (texts.isNotEmpty()) {
                    append(" [omitted]")
                }
            } else {
                if (texts.size > 1) {
                    for (i in 0 until texts.lastIndex) {
                        append(linePrefix)
                        append(replyCode)
                        if (enhancedStatusCode == null) {
                            append('-')
                        } else {
                            appendIfNotNull(enhancedStatusCode, prefix = '-')
                            append(' ')
                        }
                        append(texts[i])
                        appendLine()
                    }
                }

                append(linePrefix)
                append(replyCode)
                appendIfNotNull(enhancedStatusCode, prefix = ' ')
                if (texts.isNotEmpty()) {
                    append(' ')
                    append(texts.last())
                }
            }
        }
    }

    private fun StringBuilder.appendIfNotNull(enhancedStatusCode: EnhancedStatusCode?, prefix: Char) {
        if (enhancedStatusCode != null) {
            append(prefix)
            append(enhancedStatusCode.statusClass.codeClass)
            append('.')
            append(enhancedStatusCode.subject)
            append('.')
            append(enhancedStatusCode.detail)
        }
    }
}
