package com.fsck.k9.backend

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.pop3.Pop3Backend
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory
import com.fsck.k9.mail.store.pop3.Pop3Store
import com.fsck.k9.mailstore.K9BackendStorage

class Pop3BackendFactory(private val context: Context, private val preferences: Preferences) : BackendFactory {

    override fun createBackend(account: Account): Backend {
        val accountName = account.description
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)
        val pop3Store = createPop3Store(account)
        return Pop3Backend(accountName, backendStorage, pop3Store)
    }

    private fun createPop3Store(account: Account): Pop3Store {
        return Pop3Store(account, DefaultTrustedSocketFactory(context))
    }
}
