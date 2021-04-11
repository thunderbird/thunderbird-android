package com.fsck.k9.mailstore

import com.fsck.k9.mail.Message
import com.fsck.k9.message.extractors.PreviewResult

data class SaveMessageData(
    val message: Message,
    val subject: String?,
    val date: Long,
    val internalDate: Long,
    val partialMessage: Boolean,
    val attachmentCount: Int,
    val previewResult: PreviewResult,
    val textForSearchIndex: String? = null,
    val encryptionType: String?
)
