package com.fsck.k9.mailstore

data class OutboxState(
    val sendState: SendState,
    val numberOfSendAttempts: Int,
    val sendError: String?,
    val sendErrorTimestamp: Long,
)
