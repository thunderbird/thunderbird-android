package com.fsck.k9.backend.api

interface BackendStorage {
    fun getFolder(folderServerId: String): BackendFolder
}
