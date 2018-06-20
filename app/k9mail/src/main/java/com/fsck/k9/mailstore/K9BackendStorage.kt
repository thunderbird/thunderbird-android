package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage

class K9BackendStorage(
        private val preferences: Preferences,
        private val account: Account,
        private val localStore: LocalStore) : BackendStorage {

    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(preferences, account, localStore, folderServerId)
    }
}
