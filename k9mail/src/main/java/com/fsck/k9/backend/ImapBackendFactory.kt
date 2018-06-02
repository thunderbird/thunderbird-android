package com.fsck.k9.backend

import android.content.Context
import android.net.ConnectivityManager
import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.imap.ImapBackend
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mailstore.K9BackendStorage

class ImapBackendFactory(private val context: Context) : BackendFactory {

    override fun createBackend(account: Account): Backend {
        val accountName = account.description
        val backendStorage = K9BackendStorage(account.localStore)
        val imapStore = createImapStore(account)
        return ImapBackend(accountName, backendStorage, imapStore)
    }

    private fun createImapStore(account: Account): ImapStore {
        val oAuth2TokenProvider: OAuth2TokenProvider? = null
        return ImapStore(
                account,
                DefaultTrustedSocketFactory(context),
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
                oAuth2TokenProvider
        )
    }
}
