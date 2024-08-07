package com.fsck.k9.crypto

import android.content.ContentValues
import app.k9mail.legacy.message.extractors.PreviewResult
import com.fsck.k9.mail.Message

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
