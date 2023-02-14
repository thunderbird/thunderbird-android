package com.fsck.k9.mail.transport.smtp

class TestSmtpLogger(override val isRawProtocolLoggingEnabled: Boolean = true) : SmtpLogger {
    val logEntries = mutableListOf<LogEntry>()

    override fun log(throwable: Throwable?, message: String, vararg args: Any?) {
        val formattedMessage = String.format(message, *args)
        logEntries.add(LogEntry(throwable, formattedMessage))
    }
}

data class LogEntry(
    val throwable: Throwable?,
    val message: String,
)
