package com.fsck.k9.storage.messages

import com.fsck.k9.K9
import com.fsck.k9.helper.FileHelper
import com.fsck.k9.mailstore.StorageFilesProvider
import java.io.File
import timber.log.Timber

internal class AttachmentFileManager(
    private val storageFilesProvider: StorageFilesProvider,
) {
    fun deleteFile(messagePartId: Long) {
        val file = getAttachmentFile(messagePartId)
        if (file.exists() && !file.delete() && K9.isDebugLoggingEnabled) {
            Timber.w("Couldn't delete message part file: %s", file.absolutePath)
        }
    }

    fun moveTemporaryFile(temporaryFile: File, messagePartId: Long) {
        val destinationFile = getAttachmentFile(messagePartId)
        FileHelper.renameOrMoveByCopying(temporaryFile, destinationFile)
    }

    fun copyFile(sourceMessagePartId: Long, destinationMessagePartId: Long) {
        val sourceFile = getAttachmentFile(sourceMessagePartId)
        val destinationFile = getAttachmentFile(destinationMessagePartId)
        sourceFile.copyTo(destinationFile)
    }

    fun getAttachmentFile(messagePartId: Long): File {
        val attachmentDirectory = storageFilesProvider.getAttachmentDirectory()
        return File(attachmentDirectory, messagePartId.toString())
    }
}
