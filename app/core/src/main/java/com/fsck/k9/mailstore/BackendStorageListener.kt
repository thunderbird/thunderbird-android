package com.fsck.k9.mailstore

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

interface BackendStorageListener {
    fun onFoldersCreated(folders: List<FolderInfo>)
    fun onFoldersDeleted(folderServerIds: List<String>)
    fun onFolderChanged(folderServerId: String, name: String, type: FolderType)
}
