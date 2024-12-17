package com.fsck.k9.storage.messages

import com.fsck.k9.mailstore.DatabaseFilesProvider
import com.fsck.k9.mailstore.LockableDatabase
import timber.log.Timber

internal class DatabaseOperations(
    private val lockableDatabase: LockableDatabase,
    private val databaseFilesProvider: DatabaseFilesProvider,
) {
    fun getSize(): Long {
        val attachmentDirectory = databaseFilesProvider.getAttachmentDirectory()

        return lockableDatabase.execute(false) {
            val attachmentFiles = attachmentDirectory.listFiles() ?: emptyArray()
            val attachmentsSize = attachmentFiles.asSequence()
                .filter { file -> file.exists() }
                .fold(initial = 0L) { accumulatedSize, file ->
                    accumulatedSize + file.length()
                }

            val databaseFile = databaseFilesProvider.getDatabaseFile()
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
