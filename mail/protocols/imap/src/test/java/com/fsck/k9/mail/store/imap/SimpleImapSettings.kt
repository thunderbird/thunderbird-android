package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity

internal class SimpleImapSettings(
    private val host: String,
    private val port: Int = 0,
    private val connectionSecurity: ConnectionSecurity = ConnectionSecurity.NONE,
    private val authType: AuthType,
    private val username: String,
    private val password: String? = null,
    private val useCompression: Boolean = false
) : ImapSettings {
    private var pathPrefix: String? = null
    private var pathDelimiter: String? = null
    private var combinedPrefix: String? = null

    override fun getHost(): String = host

    override fun getPort(): Int = port

    override fun getConnectionSecurity(): ConnectionSecurity = connectionSecurity

    override fun getAuthType(): AuthType = authType

    override fun getUsername(): String = username

    override fun getPassword(): String? = password

    override fun getClientCertificateAlias(): String? = null

    override fun useCompression(): Boolean = useCompression

    override fun getPathPrefix(): String? = pathPrefix

    override fun setPathPrefix(prefix: String?) {
        pathPrefix = prefix
    }

    override fun getPathDelimiter(): String? = pathDelimiter

    override fun setPathDelimiter(delimiter: String?) {
        pathDelimiter = delimiter
    }

    override fun setCombinedPrefix(prefix: String?) {
        combinedPrefix = prefix
    }
}
