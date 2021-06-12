package com.fsck.k9.backend.api

interface BackendPusher {
    fun updateFolders(folderServerIds: Collection<String>)
    fun stop()
}
