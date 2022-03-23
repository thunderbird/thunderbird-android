package com.fsck.k9.mail.transport.smtp

internal data class SmtpResponse(
    val replyCode: Int,
    val statusCode: StatusCode?,
    val texts: List<String>
) {
    val isNegativeResponse = replyCode >= 400

    val joinedText: String
        get() = texts.joinToString(separator = " ")

    fun toLogString(omitText: Boolean, linePrefix: String): String {
        return buildString {
            if (omitText) {
                append(linePrefix)
                append(replyCode)
                appendIfNotNull(statusCode, prefix = ' ')
                if (texts.isNotEmpty()) {
                    append(" [omitted]")
                }
            } else {
                if (texts.size > 1) {
                    for (i in 0 until texts.lastIndex) {
                        append(linePrefix)
                        append(replyCode)
                        if (statusCode == null) {
                            append('-')
                        } else {
                            appendIfNotNull(statusCode, prefix = '-')
                            append(' ')
                        }
                        append(texts[i])
                        appendLine()
                    }
                }

                append(linePrefix)
                append(replyCode)
                appendIfNotNull(statusCode, prefix = ' ')
                if (texts.isNotEmpty()) {
                    append(' ')
                    append(texts.last())
                }
            }
        }
    }

    private fun StringBuilder.appendIfNotNull(statusCode: StatusCode?, prefix: Char) {
        if (statusCode != null) {
            append(prefix)
            append(statusCode.statusClass.codeClass)
            append('.')
            append(statusCode.subject)
            append('.')
            append(statusCode.detail)
        }
    }
}

internal data class StatusCode(
    val statusClass: StatusCodeClass,
    val subject: Int,
    val detail: Int
)
