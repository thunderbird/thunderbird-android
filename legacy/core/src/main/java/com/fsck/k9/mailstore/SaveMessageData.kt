package com.fsck.k9.mailstore

import app.k9mail.legacy.message.extractors.PreviewResult
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState

data class SaveMessageData(
    val message: Message,
    val subject: String?,
    val date: Long,
    val internalDate: Long,
    val downloadState: MessageDownloadState,
    val attachmentCount: Int,
    val previewResult: PreviewResult,
    val textForSearchIndex: String? = null,
    val encryptionType: String?,
)
