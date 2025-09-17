package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.ImapClientInfo
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.ImapStoreConfig
import com.fsck.k9.mail.store.imap.ImapStoreFactory
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import org.intellij.lang.annotations.Language
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

private const val TAG = "MigrationTo90"

internal class MigrationTo90(
    private val db: SQLiteDatabase,
    private val migrationsHelper: MigrationsHelper,
    private val logger: Logger = Log,
    private val imapStoreFactory: ImapStoreFactory = ImapStore.Companion,
) : KoinComponent {
    private val trustedSocketFactory: TrustedSocketFactory by inject()
    private val clientInfoAppName: String by inject(named("ClientInfoAppName"))
    private val clientInfoAppVersion: String by inject(named("ClientInfoAppVersion"))
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory by inject()

    fun removeImapPrefixFromFolderServerId() {
        val account = migrationsHelper.account
        if (account.incomingServerSettings.type != Protocols.IMAP) {
            logger.verbose(TAG) {
                "account ${account.uuid} is not an IMAP account, skipping db migration for this account."
            }
            return
        }

        logger.verbose(TAG) { "started db migration to version 90 to account ${account.uuid}" }

        val imapStore = createImapStore(account)

        try {
            logger.verbose(TAG) { "fetching IMAP prefix" }
            imapStore.fetchImapPrefix()
        } catch (e: AuthenticationFailedException) {
            logger.warn(TAG, e) { "failed to fetch IMAP prefix. skipping db migration" }
            return
        }

        val imapPrefix = imapStore.combinedPrefix

        if (imapPrefix?.isNotBlank() == true) {
            logger.verbose(TAG) { "Imap Prefix ($imapPrefix) detected, updating folder's server_id" }
            val query = buildQuery(imapPrefix)
            db.execSQL(query)
        } else {
            logger.verbose(TAG) { "No Imap Prefix detected, skipping db migration" }
        }

        logger.verbose(TAG) { "completed db migration to version 90 for account ${account.uuid}" }
    }

    private fun createImapStore(account: LegacyAccountDto): ImapStore {
        val serverSettings = account.toImapServerSettings()
        val oAuth2TokenProvider = if (serverSettings.authenticationType == AuthType.XOAUTH2) {
            val authStateStorage = object : AuthStateStorage {
                override fun getAuthorizationState(): String? = account.oAuthState
                override fun updateAuthorizationState(authorizationState: String?) = Unit
            }
            oAuth2TokenProviderFactory.create(authStateStorage)
        } else {
            null
        }

        return imapStoreFactory.create(
            serverSettings = serverSettings,
            config = createImapStoreConfig(account),
            trustedSocketFactory = trustedSocketFactory,
            oauthTokenProvider = oAuth2TokenProvider,
        )
    }

    private fun LegacyAccountDto.toImapServerSettings(): ServerSettings {
        val serverSettings = incomingServerSettings
        return serverSettings.copy(
            extra = ImapStoreSettings.createExtra(
                autoDetectNamespace = serverSettings.autoDetectNamespace,
                pathPrefix = serverSettings.pathPrefix,
                useCompression = useCompression,
                sendClientInfo = isSendClientInfoEnabled,
            ),
        )
    }

    private fun createImapStoreConfig(account: LegacyAccountDto): ImapStoreConfig {
        return object : ImapStoreConfig {
            override val logLabel
                get() = account.uuid

            override fun isSubscribedFoldersOnly() = account.isSubscribedFoldersOnly

            override fun isExpungeImmediately() = account.expungePolicy == Expunge.EXPUNGE_IMMEDIATELY

            override fun clientInfo() = ImapClientInfo(appName = clientInfoAppName, appVersion = clientInfoAppVersion)
        }
    }

    @Language("RoomSql")
    @VisibleForTesting
    internal fun buildQuery(imapPrefix: String): String {
        return """
            |UPDATE folders
            |    SET server_id = REPLACE(server_id, '$imapPrefix', '')
            |WHERE
            |    server_id LIKE '$imapPrefix%'
        """.trimMargin()
    }
}
