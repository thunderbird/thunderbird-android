package com.fsck.k9.backend.api

interface BackendStorage {
    fun getFolder(folderServerId: String): BackendFolder

    fun getFolderServerIds(): List<String>

    fun createFolders(folders: List<FolderInfo>)
    fun deleteFolders(folderServerIds: List<String>)
    fun changeFolder(folderServerId: String, name: String)

    fun getExtraString(name: String): String?
    fun setExtraString(name: String, value: String)
    fun getExtraNumber(name: String): Long?
    fun setExtraNumber(name: String, value: Long)
}
