package com.fsck.k9.backend.api

interface BackendPusher {
    fun start()
    fun updateFolders(folderServerIds: Collection<String>)
    fun stop()
    fun reconnect()
}
