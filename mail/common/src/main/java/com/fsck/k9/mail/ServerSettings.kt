package com.fsck.k9.mail

import java.util.Locale

/**
 * Container for incoming or outgoing server settings
 */
data class ServerSettings @JvmOverloads constructor(
    @JvmField val type: String,
    @JvmField val host: String?,
    @JvmField val port: Int,
    @JvmField val connectionSecurity: ConnectionSecurity,
    @JvmField val authenticationType: AuthType,
    @JvmField val username: String,
    @JvmField val password: String?,
    @JvmField val clientCertificateAlias: String?,
    val extra: Map<String, String?> = emptyMap()
) {
    init {
        require(type == type.toLowerCase(Locale.ROOT)) { "type must be all lower case" }
    }

    fun newPassword(newPassword: String?): ServerSettings {
        return this.copy(password = newPassword)
    }
}
