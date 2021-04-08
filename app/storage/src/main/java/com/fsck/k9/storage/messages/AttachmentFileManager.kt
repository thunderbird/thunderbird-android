package com.fsck.k9.storage.messages

import com.fsck.k9.K9
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.mailstore.StorageManager.InternalStorageProvider
import java.io.File
import timber.log.Timber

class AttachmentFileManager(
    private val storageManager: StorageManager,
    private val accountUuid: String
) {
    fun deleteFile(messagePartId: Long) {
        val file = getAttachmentFile(messagePartId.toString())
        if (file.exists() && !file.delete() && K9.isDebugLoggingEnabled) {
            Timber.w("Couldn't delete message part file: %s", file.absolutePath)
        }
    }

    private fun getAttachmentFile(messagePartId: String): File {
        val attachmentDirectory = storageManager.getAttachmentDirectory(accountUuid, InternalStorageProvider.ID)
        return File(attachmentDirectory, messagePartId)
    }
}
