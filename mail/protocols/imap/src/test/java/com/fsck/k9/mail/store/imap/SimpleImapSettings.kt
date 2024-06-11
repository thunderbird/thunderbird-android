package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity

@Suppress("LongParameterList")
internal class SimpleImapSettings(
    override val host: String,
    override val port: Int = 0,
    override val connectionSecurity: ConnectionSecurity = ConnectionSecurity.NONE,
    override val authType: AuthType,
    override val username: String,
    override val password: String? = null,
    override val useCompression: Boolean = false,
    override val clientInfo: ImapClientInfo? = null,
) : ImapSettings {
    override val clientCertificateAlias: String? = null

    override var pathPrefix: String? = null
    override var pathDelimiter: String? = null

    override fun setCombinedPrefix(prefix: String?) = Unit
}
