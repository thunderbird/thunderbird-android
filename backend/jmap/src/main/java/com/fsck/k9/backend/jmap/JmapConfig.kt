package com.fsck.k9.backend.jmap

data class JmapConfig(
    val username: String,
    val password: String,
    val baseUrl: String?,
    val accountId: String,
)
