package com.fsck.k9.backend.api

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessagingException
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
    @Throws(MessagingException::class)
    fun createFolders(folders: List<FolderInfo>): Set<Long>
    fun deleteFolders(folderServerIds: List<String>)

    @Throws(MessagingException::class)
    fun changeFolder(folderServerId: String, name: String, type: FolderType)
}

fun BackendFolderUpdater.createFolder(folder: FolderInfo): Long? = createFolders(listOf(folder)).firstOrNull()

inline fun <T> BackendStorage.updateFolders(block: BackendFolderUpdater.() -> T): T {
    return createFolderUpdater().use { it.block() }
}
