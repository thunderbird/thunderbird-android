package com.fsck.k9.backends

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.gmailapi.GmailBackend
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

interface GmailBackendFactory : BackendFactory

class DefaultGmailBackendFactory(
    private val accountManager: LegacyAccountManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory,
    private val logger: Logger,
) : GmailBackendFactory {
    override fun createBackend(accountId: AccountId): Backend {
        val account = accountManager.getById(accountId)
            ?: error("Account not found: $accountId")
        val incomingSettings = account.incomingServerSettings
        require(incomingSettings.type == Protocols.GMAIL_API) {
            "Expected a Gmail REST account, got ${incomingSettings.type}"
        }
        require(incomingSettings.authenticationType == AuthType.XOAUTH2) {
            "Gmail REST accounts require OAuth2 authentication"
        }

        val tokenProvider = oAuth2TokenProviderFactory.create(AccountAuthStateStorage(accountManager, accountId))
        val storage = backendStorageFactory.createBackendStorage(accountId)
        return GmailBackend(storage, tokenProvider, logger)
    }
}
