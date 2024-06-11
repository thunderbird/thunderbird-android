package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderFetcher
import com.fsck.k9.mail.folders.FolderFetcherException
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory

/**
 * Fetches the list of folders from an IMAP server.
 */
class ImapFolderFetcher internal constructor(
    private val trustedSocketFactory: TrustedSocketFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory?,
    private val clientInfoAppName: String,
    private val clientInfoAppVersion: String,
    private val imapStoreFactory: ImapStoreFactory,
) : FolderFetcher {
    constructor(
        trustedSocketFactory: TrustedSocketFactory,
        oAuth2TokenProviderFactory: OAuth2TokenProviderFactory?,
        clientInfoAppName: String,
        clientInfoAppVersion: String,
    ) : this(
        trustedSocketFactory,
        oAuth2TokenProviderFactory,
        clientInfoAppName,
        clientInfoAppVersion,
        imapStoreFactory = ImapStore.Companion,
    )

    @Suppress("TooGenericExceptionCaught")
    override fun getFolders(serverSettings: ServerSettings, authStateStorage: AuthStateStorage?): List<RemoteFolder> {
        require(serverSettings.type == "imap")

        val config = object : ImapStoreConfig {
            override val logLabel = "folder-fetcher"
            override fun isSubscribedFoldersOnly() = false
            override fun isExpungeImmediately() = false
            override fun clientInfo() = ImapClientInfo(appName = clientInfoAppName, appVersion = clientInfoAppVersion)
        }
        val oAuth2TokenProvider = createOAuth2TokenProviderOrNull(authStateStorage)
        val store = imapStoreFactory.create(serverSettings, config, trustedSocketFactory, oAuth2TokenProvider)

        return try {
            store.getFolders()
                .asSequence()
                .filterNot { it.oldServerId == null }
                .map { folder ->
                    RemoteFolder(
                        serverId = FolderServerId(folder.oldServerId!!),
                        displayName = folder.name,
                        type = folder.type,
                    )
                }
                .toList()
        } catch (e: AuthenticationFailedException) {
            throw FolderFetcherException(messageFromServer = e.messageFromServer, cause = e)
        } catch (e: NegativeImapResponseException) {
            throw FolderFetcherException(messageFromServer = e.responseText, cause = e)
        } catch (e: Exception) {
            throw FolderFetcherException(cause = e)
        } finally {
            store.closeAllConnections()
        }
    }

    private fun createOAuth2TokenProviderOrNull(authStateStorage: AuthStateStorage?): OAuth2TokenProvider? {
        return authStateStorage?.let {
            oAuth2TokenProviderFactory?.create(it)
        }
    }
}
