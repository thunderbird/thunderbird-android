package com.fsck.k9.controller

interface BackendStorage {
    fun getFolder(folderServerId: String): BackendFolder
}
