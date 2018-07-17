package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.webdav.WebDavBackend
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.webdav.WebDavStore
import com.fsck.k9.mail.store.webdav.WebDavStoreSettings
import com.fsck.k9.backend.webdav.WebDavStoreUriCreator
import com.fsck.k9.backend.webdav.WebDavStoreUriDecoder
import com.fsck.k9.mail.transport.WebDavTransport
import com.fsck.k9.mailstore.K9BackendStorage

class WebDavBackendFactory(private val preferences: Preferences) : BackendFactory {
    override val transportUriPrefix = "webdav"

    override fun createBackend(account: Account): Backend {
        val accountName = account.description
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)
        val serverSettings = WebDavStoreUriDecoder.decode(account.storeUri)
        val webDavStore = createWebDavStore(serverSettings, account)
        val webDavTransport = WebDavTransport(serverSettings, account)
        return WebDavBackend(accountName, backendStorage, webDavStore, webDavTransport)
    }

    private fun createWebDavStore(serverSettings: WebDavStoreSettings, account: Account): WebDavStore {
        return WebDavStore(serverSettings, account)
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
