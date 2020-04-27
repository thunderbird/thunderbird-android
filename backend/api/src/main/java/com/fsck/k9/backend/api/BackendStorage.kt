package com.fsck.k9.backend.api

import com.fsck.k9.mail.FolderType
import java.io.Closeable

interface BackendStorage {
    fun getFolder(folderServerId: String): BackendFolder

    fun getFolderServerIds(): List<String>

    fun createFolderUpdater(): BackendFolderUpdater

    fun getExtraString(name: String): String?
    fun setExtraString(name: String, value: String)
    fun getExtraNumber(name: String): Long?
    fun setExtraNumber(name: String, value: Long)
}

interface BackendFolderUpdater : Closeable {
    fun createFolders(folders: List<FolderInfo>)
    fun deleteFolders(folderServerIds: List<String>)
    fun changeFolder(folderServerId: String, name: String, type: FolderType)
}

inline fun BackendStorage.updateFolders(block: BackendFolderUpdater.() -> Unit) {
    createFolderUpdater().use { it.block() }
}
