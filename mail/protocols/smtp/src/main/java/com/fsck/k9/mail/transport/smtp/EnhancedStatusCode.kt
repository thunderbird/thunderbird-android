package com.fsck.k9.mail.transport.smtp

data class EnhancedStatusCode(
    val statusClass: StatusCodeClass,
    val subject: Int,
    val detail: Int,
)
