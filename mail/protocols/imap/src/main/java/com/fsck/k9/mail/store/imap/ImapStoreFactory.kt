package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory

internal fun interface ImapStoreFactory {
    fun create(
        serverSettings: ServerSettings,
        config: ImapStoreConfig,
        trustedSocketFactory: TrustedSocketFactory,
        oauthTokenProvider: OAuth2TokenProvider?,
    ): ImapStore
}
