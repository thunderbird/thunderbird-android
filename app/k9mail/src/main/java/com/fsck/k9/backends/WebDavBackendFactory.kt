package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.webdav.WebDavBackend
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.store.webdav.DraftsFolderProvider
import com.fsck.k9.mail.store.webdav.SniHostSetter
import com.fsck.k9.mail.store.webdav.WebDavStore
import com.fsck.k9.mail.transport.WebDavTransport
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.mailstore.K9BackendStorageFactory

class WebDavBackendFactory(
    private val backendStorageFactory: K9BackendStorageFactory,
    private val trustManagerFactory: TrustManagerFactory,
    private val sniHostSetter: SniHostSetter,
    private val folderRepository: FolderRepository
) : BackendFactory {
    override fun createBackend(account: Account): Backend {
        val accountName = account.displayName
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val serverSettings = account.incomingServerSettings
        val draftsFolderProvider = createDraftsFolderProvider(account)
        val webDavStore = WebDavStore(trustManagerFactory, sniHostSetter, serverSettings, draftsFolderProvider)
        val webDavTransport = WebDavTransport(trustManagerFactory, sniHostSetter, serverSettings, draftsFolderProvider)
        return WebDavBackend(accountName, backendStorage, webDavStore, webDavTransport)
    }

    private fun createDraftsFolderProvider(account: Account): DraftsFolderProvider {
        return DraftsFolderProvider {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")
            folderRepository.getFolderServerId(account, draftsFolderId) ?: error("Couldn't find local Drafts folder")
        }
    }
}
