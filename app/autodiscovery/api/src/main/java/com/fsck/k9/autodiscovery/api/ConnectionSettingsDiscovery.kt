package com.fsck.k9.autodiscovery.api

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity

interface ConnectionSettingsDiscovery {
    fun discover(email: String, target: DiscoveryTarget): DiscoveryResults?
}

enum class DiscoveryTarget(val outgoing: Boolean, val incoming: Boolean) {
    OUTGOING(true, false),
    INCOMING(false, true),
    INCOMING_AND_OUTGOING(true, true)
}

data class DiscoveryResults(val incoming: List<DiscoveredServerSettings>, val outgoing: List<DiscoveredServerSettings>)

data class DiscoveredServerSettings(
    val protocol: String,
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
    val authType: AuthType?,
    val username: String?
)
