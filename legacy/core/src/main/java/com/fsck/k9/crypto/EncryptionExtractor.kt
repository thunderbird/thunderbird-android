package com.fsck.k9.crypto

import android.content.ContentValues
import com.fsck.k9.mail.Message
import com.fsck.k9.message.extractors.PreviewResult

interface EncryptionExtractor {
    fun extractEncryption(message: Message): EncryptionResult?
}

data class EncryptionResult(
    val encryptionType: String,
    val attachmentCount: Int,
    val previewResult: PreviewResult = PreviewResult.encrypted(),
    val textForSearchIndex: String? = null,
    val extraContentValues: ContentValues? = null,
)
