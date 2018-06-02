package com.fsck.k9.mailstore

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage

class K9BackendStorage(private val localStore: LocalStore) : BackendStorage {

    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(localStore, folderServerId)
    }
}
