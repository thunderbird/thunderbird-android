package com.fsck.k9.backends

import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.pop3.Pop3Backend
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.pop3.Pop3Store
import com.fsck.k9.mail.transport.smtp.SmtpTransport
import com.fsck.k9.mailstore.K9BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccount

class Pop3BackendFactory(
    private val backendStorageFactory: K9BackendStorageFactory,
    private val trustedSocketFactory: TrustedSocketFactory,
) : BackendFactory {
    override fun createBackend(account: LegacyAccount): Backend {
        val accountName = account.displayName
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val pop3Store = createPop3Store(account)
        val smtpTransport = createSmtpTransport(account)
        return Pop3Backend(accountName, backendStorage, pop3Store, smtpTransport)
    }

    private fun createPop3Store(account: LegacyAccount): Pop3Store {
        val serverSettings = account.incomingServerSettings
        return Pop3Store(serverSettings, trustedSocketFactory)
    }

    private fun createSmtpTransport(account: LegacyAccount): SmtpTransport {
        val serverSettings = account.outgoingServerSettings
        val oauth2TokenProvider: OAuth2TokenProvider? = null
        return SmtpTransport(serverSettings, trustedSocketFactory, oauth2TokenProvider)
    }
}
