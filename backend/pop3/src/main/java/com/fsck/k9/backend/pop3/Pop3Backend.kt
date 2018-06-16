package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.store.pop3.Pop3Store

class Pop3Backend(accountName: String, backendStorage: BackendStorage, pop3Store: Pop3Store) : Backend {
    private val pop3Sync: Pop3Sync = Pop3Sync(accountName, backendStorage, pop3Store)

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        pop3Sync.sync(folder, syncConfig, listener)
    }
}
