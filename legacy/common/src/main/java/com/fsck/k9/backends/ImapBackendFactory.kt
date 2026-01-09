package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.imap.ImapBackend
import com.fsck.k9.backend.imap.ImapPushConfigProvider
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.ImapClientInfo
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.ImapStoreConfig
import com.fsck.k9.mail.transport.smtp.SmtpTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

interface ImapBackendFactory : BackendFactory

@Suppress("LongParameterList")
class DefaultImapBackendFactory(
    private val accountManager: LegacyAccountManager,
    private val powerManager: PowerManager,
    private val idleRefreshManager: IdleRefreshManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val trustedSocketFactory: TrustedSocketFactory,
    private val context: Context,
    private val clientInfoAppName: String,
    private val clientInfoAppVersion: String,
) : ImapBackendFactory {
    override fun createBackend(accountId: AccountId): Backend {
        val account = accountManager.getAccount(accountId.toString()) ?: error("Account not found: $accountId")
        // TODO: should we pass the account name or userId here?
        val accountName = account.profile.name
        val backendStorage = backendStorageFactory.createBackendStorage(accountId)
        val imapStore = createImapStore(account)
        val pushConfigProvider = createPushConfigProvider(account)
        val smtpTransport = createSmtpTransport(account)

        return ImapBackend(
            accountName,
            backendStorage,
            imapStore,
            powerManager,
            idleRefreshManager,
            pushConfigProvider,
            smtpTransport,
        )
    }

    private fun createImapStore(account: LegacyAccount): ImapStore {
        val serverSettings = account.toImapServerSettings()

        val oAuth2TokenProvider = if (serverSettings.authenticationType == AuthType.XOAUTH2) {
            createOAuth2TokenProvider(account.id)
        } else {
            null
        }

        val config = createImapStoreConfig(account)
        return ImapStore.create(
            serverSettings,
            config,
            trustedSocketFactory,
            oAuth2TokenProvider,
        )
    }

    private fun createImapStoreConfig(account: LegacyAccount): ImapStoreConfig {
        return object : ImapStoreConfig {
            override val logLabel
                get() = account.uuid

            override fun isSubscribedFoldersOnly() = account.isSubscribedFoldersOnly

            override fun isExpungeImmediately() = account.expungePolicy == Expunge.EXPUNGE_IMMEDIATELY

            override fun clientInfo() = ImapClientInfo(appName = clientInfoAppName, appVersion = clientInfoAppVersion)
        }
    }

    private fun createSmtpTransport(account: LegacyAccount): SmtpTransport {
        val serverSettings = account.outgoingServerSettings
        val oauth2TokenProvider = if (serverSettings.authenticationType == AuthType.XOAUTH2) {
            createOAuth2TokenProvider(account.id)
        } else {
            null
        }

        return SmtpTransport(serverSettings, trustedSocketFactory, oauth2TokenProvider)
    }

    private fun createOAuth2TokenProvider(accountId: AccountId): RealOAuth2TokenProvider {
        val authStateStorage = AccountAuthStateStorage(accountManager, accountId)
        return RealOAuth2TokenProvider(context, authStateStorage)
    }

    private fun createPushConfigProvider(account: LegacyAccount) = object : ImapPushConfigProvider {
        override val maxPushFoldersFlow: Flow<Int>
            get() = accountManager.getAccountFlow(account.uuid)
                .filterNotNull()
                .map { it.maxPushFolders }
                .distinctUntilChanged()

        override val idleRefreshMinutesFlow: Flow<Int>
            get() = accountManager.getAccountFlow(account.uuid)
                .filterNotNull()
                .map { it.idleRefreshMinutes }
                .distinctUntilChanged()
    }
}
