package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity

/**
 * Settings source for IMAP. Implemented in order to remove coupling between [ImapStore] and [ImapConnection].
 */
internal interface ImapSettings {
    val host: String
    val port: Int
    val connectionSecurity: ConnectionSecurity
    val authType: AuthType
    val username: String
    val password: String?
    val clientCertificateAlias: String?
    val useCompression: Boolean
    val clientInfo: ImapClientInfo?

    var pathPrefix: String?
    var pathDelimiter: String?
    fun setCombinedPrefix(prefix: String?)
}
