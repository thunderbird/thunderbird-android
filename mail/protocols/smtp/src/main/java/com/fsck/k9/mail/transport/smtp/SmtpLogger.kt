package com.fsck.k9.mail.transport.smtp

interface SmtpLogger {
    val isRawProtocolLoggingEnabled: Boolean

    fun log(message: String, vararg args: Any?) = log(throwable = null, message, *args)

    fun log(throwable: Throwable?, message: String, vararg args: Any?)
}
