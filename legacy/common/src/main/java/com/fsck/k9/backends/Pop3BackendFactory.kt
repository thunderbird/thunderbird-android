package com.fsck.k9.backends

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.pop3.Pop3Backend
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.pop3.Pop3Store
import com.fsck.k9.mail.transport.smtp.SmtpTransport
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

interface Pop3BackendFactory : BackendFactory

class DefaultPop3BackendFactory(
    private val accountManager: LegacyAccountManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val trustedSocketFactory: TrustedSocketFactory,
) : Pop3BackendFactory {

    override fun createBackend(accountId: AccountId): Backend {
        val account = accountManager.getAccount(accountId.asRaw())
            ?: error("Account not found: $accountId")
        // TODO: should we pass the account name or userId here?
        val accountName = account.profile.name
        val backendStorage = backendStorageFactory.createBackendStorage(accountId)
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
