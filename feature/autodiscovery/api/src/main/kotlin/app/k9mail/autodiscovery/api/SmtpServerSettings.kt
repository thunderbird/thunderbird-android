package app.k9mail.autodiscovery.api

import net.thunderbird.core.common.net.Hostname
import net.thunderbird.core.common.net.Port

data class SmtpServerSettings(
    val hostname: Hostname,
    val port: Port,
    val connectionSecurity: ConnectionSecurity,
    val authenticationTypes: List<AuthenticationType>,
    val username: String,
) : OutgoingServerSettings
