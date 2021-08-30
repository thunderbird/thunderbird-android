package com.fsck.k9.mail

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
    val isMissingCredentials: Boolean = when (authenticationType) {
        AuthType.EXTERNAL -> clientCertificateAlias == null
        else -> username.isNotBlank() && password == null
    }

    init {
        require(type == type.lowercase()) { "type must be all lower case" }
    }

    fun newPassword(newPassword: String?): ServerSettings {
        return this.copy(password = newPassword)
    }
}
