package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.webdav.WebDavBackend
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.webdav.WebDavStore
import com.fsck.k9.mail.store.webdav.WebDavStoreUriCreator
import com.fsck.k9.mail.store.webdav.WebDavStoreUriDecoder
import com.fsck.k9.mail.transport.WebDavTransport
import com.fsck.k9.mailstore.K9BackendStorage

class WebDavBackendFactory(private val preferences: Preferences) : BackendFactory {
    override val transportUriPrefix = "webdav"

    override fun createBackend(account: Account): Backend {
        val accountName = account.description
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)
        val webDavStore = createWebDavStore(account)
        val webDavTransport = WebDavTransport(account)
        return WebDavBackend(accountName, backendStorage, webDavStore, webDavTransport)
    }

    private fun createWebDavStore(account: Account): WebDavStore {
        return WebDavStore(account)
    }

    override fun decodeStoreUri(storeUri: String): ServerSettings {
        return WebDavStoreUriDecoder.decode(storeUri)
    }

    override fun createStoreUri(serverSettings: ServerSettings): String {
        return WebDavStoreUriCreator.create(serverSettings)
    }

    override fun decodeTransportUri(transportUri: String): ServerSettings {
        return WebDavStoreUriDecoder.decode(transportUri)
    }

    override fun createTransportUri(serverSettings: ServerSettings): String {
        return WebDavStoreUriCreator.create(serverSettings)
    }
}
