package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.webdav.WebDavBackend
import com.fsck.k9.mail.store.webdav.WebDavStore
import com.fsck.k9.mailstore.K9BackendStorage

class WebDavBackendFactory(private val preferences: Preferences) : BackendFactory {

    override fun createBackend(account: Account): Backend {
        val accountName = account.description
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)
        val webDavStore = createWebDavStore(account)
        return WebDavBackend(accountName, backendStorage, webDavStore)
    }

    private fun createWebDavStore(account: Account): WebDavStore {
        return WebDavStore(account)
    }
}
