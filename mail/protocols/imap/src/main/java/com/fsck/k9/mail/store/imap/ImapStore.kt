package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import net.thunderbird.core.common.exception.MessagingException

interface ImapStore {
    /**
     * The IMAP prefix combined with the Path delimiter given by the server.
     */
    val combinedPrefix: String?

    @Throws(MessagingException::class)
    fun checkSettings()

    fun getFolder(name: String): ImapFolder

    @Throws(MessagingException::class)
    fun getFolders(): List<FolderListItem>

    fun closeAllConnections()

    fun fetchImapPrefix()

    companion object : ImapStoreFactory {
        override fun create(
            serverSettings: ServerSettings,
            config: ImapStoreConfig,
            trustedSocketFactory: TrustedSocketFactory,
            oauthTokenProvider: OAuth2TokenProvider?,
        ): ImapStore {
            return RealImapStore(serverSettings, config, trustedSocketFactory, oauthTokenProvider)
        }
    }
}
