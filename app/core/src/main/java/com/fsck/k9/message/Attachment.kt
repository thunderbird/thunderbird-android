package com.fsck.k9.message

interface Attachment {
    val state: LoadingState
    val fileName: String?
    val contentType: String?
    val name: String?
    val size: Long?
    val isInternalAttachment: Boolean

    enum class LoadingState {
        URI_ONLY,
        METADATA,
        COMPLETE,
        CANCELLED,
    }
}
