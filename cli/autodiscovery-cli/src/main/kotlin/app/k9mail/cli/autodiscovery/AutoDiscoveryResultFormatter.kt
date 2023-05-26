package app.k9mail.cli.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings

internal class AutoDiscoveryResultFormatter(private val echo: (String) -> Unit) {
    fun output(discoveryResult: AutoDiscoveryResult) {
        val incomingServer = requireNotNull(discoveryResult.incomingServerSettings as? ImapServerSettings)
        val outgoingServer = requireNotNull(discoveryResult.outgoingServerSettings as? SmtpServerSettings)

        echo("------------------------------")
        echo("Incoming server:")
        echo("  Hostname:            ${incomingServer.hostname.value}")
        echo("  Port:                ${incomingServer.port.value}")
        echo("  Connection security: ${incomingServer.connectionSecurity}")
        echo("  Authentication type: ${incomingServer.authenticationType}")
        echo("  Username:            ${incomingServer.username}")
        echo("")
        echo("Outgoing server:")
        echo("  Hostname:            ${outgoingServer.hostname.value}")
        echo("  Port:                ${outgoingServer.port.value}")
        echo("  Connection security: ${outgoingServer.connectionSecurity}")
        echo("  Authentication type: ${outgoingServer.authenticationType}")
        echo("  Username:            ${outgoingServer.username}")
        echo("------------------------------")
    }
}
