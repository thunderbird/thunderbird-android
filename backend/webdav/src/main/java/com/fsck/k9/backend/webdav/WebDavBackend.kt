package com.fsck.k9.backend.webdav

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.store.webdav.WebDavStore

class WebDavBackend(accountName: String, backendStorage: BackendStorage, webDavStore: WebDavStore) : Backend {
    private val webDavSync: WebDavSync = WebDavSync(accountName, backendStorage, webDavStore)

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        webDavSync.sync(folder, syncConfig, listener)
    }
}
