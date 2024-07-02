package com.fsck.k9.storage.messages

import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.StorageManager
import timber.log.Timber

internal class DatabaseOperations(
    private val lockableDatabase: LockableDatabase,
    val storageManager: StorageManager,
    val accountUuid: String,
) {
    fun getSize(): Long {
        val storageProviderId = lockableDatabase.storageProviderId
        val attachmentDirectory = storageManager.getAttachmentDirectory(accountUuid, storageProviderId)

        return lockableDatabase.execute(false) {
            val attachmentFiles = attachmentDirectory.listFiles() ?: emptyArray()
            val attachmentsSize = attachmentFiles.asSequence()
                .filter { file -> file.exists() }
                .fold(initial = 0L) { accumulatedSize, file ->
                    accumulatedSize + file.length()
                }

            val databaseFile = storageManager.getDatabase(accountUuid, storageProviderId)
            val databaseSize = databaseFile.length()

            databaseSize + attachmentsSize
        }
    }

    fun compact() {
        Timber.i("Before compaction size = %d", getSize())

        lockableDatabase.execute(false) { database ->
            database.execSQL("VACUUM")
        }

        Timber.i("After compaction size = %d", getSize())
    }
}
