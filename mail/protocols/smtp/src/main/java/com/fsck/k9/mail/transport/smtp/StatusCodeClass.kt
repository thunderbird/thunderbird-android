package com.fsck.k9.mail.transport.smtp

enum class StatusCodeClass(val codeClass: Int) {
    SUCCESS(2),
    PERSISTENT_TRANSIENT_FAILURE(4),
    PERMANENT_FAILURE(5),
}
