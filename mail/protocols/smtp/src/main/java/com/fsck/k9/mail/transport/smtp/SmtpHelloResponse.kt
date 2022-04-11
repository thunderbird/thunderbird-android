package com.fsck.k9.mail.transport.smtp

internal sealed interface SmtpHelloResponse {
    val response: SmtpResponse

    data class Error(override val response: SmtpResponse) : SmtpHelloResponse
    data class Hello(override val response: SmtpResponse, val keywords: Map<String, List<String>>) : SmtpHelloResponse
}
