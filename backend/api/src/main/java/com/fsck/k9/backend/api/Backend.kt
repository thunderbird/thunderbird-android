package com.fsck.k9.backend.api


import com.fsck.k9.mail.Folder


interface Backend {
    // TODO: Add a way to cancel the sync process
    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>)
}
