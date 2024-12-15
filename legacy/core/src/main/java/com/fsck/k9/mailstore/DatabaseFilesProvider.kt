package com.fsck.k9.mailstore

import java.io.File

interface DatabaseFilesProvider {
    /**
     * Returns the file that should be used for the message store database.
     */
    fun getDatabaseFile(): File

    /**
     * Returns the directory under which to store attachments.
     */
    fun getAttachmentDirectory(): File
}
