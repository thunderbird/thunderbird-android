package com.fsck.k9.backends

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.graph.GraphBackend
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

interface GraphBackendFactory : BackendFactory

class DefaultGraphBackendFactory(
    private val accountManager: LegacyAccountManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory,
    private val logger: Logger,
) : GraphBackendFactory {
    override fun createBackend(accountId: AccountId): Backend {
        val account = accountManager.getById(accountId)
            ?: error("Account not found: $accountId")
        val incomingSettings = account.incomingServerSettings
        require(Protocols.isGraphProtocol(incomingSettings.type)) {
            "Expected a Graph account, got ${incomingSettings.type}"
        }
        require(incomingSettings.authenticationType == AuthType.XOAUTH2) {
            "Microsoft Graph accounts require OAuth2 authentication"
        }

        val authStateStorage = AccountAuthStateStorage(accountManager, accountId)
        val tokenProvider = oAuth2TokenProviderFactory.create(authStateStorage)
        val backendStorage = backendStorageFactory.createBackendStorage(accountId)
        return GraphBackend(backendStorage, tokenProvider, logger)
    }
}
