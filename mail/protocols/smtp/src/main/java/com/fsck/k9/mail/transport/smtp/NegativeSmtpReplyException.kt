package com.fsck.k9.mail.transport.smtp

import com.fsck.k9.mail.MessagingException

/**
 * Exception that is thrown when the server sends a negative reply (reply codes 4xx or 5xx).
 */
open class NegativeSmtpReplyException(
    val replyCode: Int,
    val replyText: String,
    val enhancedStatusCode: EnhancedStatusCode? = null,
) : MessagingException(
    buildErrorMessage(replyCode, replyText),
    isPermanentSmtpError(replyCode),
)

private fun buildErrorMessage(replyCode: Int, replyText: String): String {
    return replyText.ifEmpty { "Negative SMTP reply: $replyCode" }
}

private fun isPermanentSmtpError(replyCode: Int): Boolean {
    return replyCode in 500..599
}
