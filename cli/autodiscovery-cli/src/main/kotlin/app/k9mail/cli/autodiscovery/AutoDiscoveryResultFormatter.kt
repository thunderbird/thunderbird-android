package app.k9mail.cli.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult.Settings
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings

internal class AutoDiscoveryResultFormatter(private val echo: (String) -> Unit) {
    fun output(settings: Settings) {
        val incomingServer = requireNotNull(settings.incomingServerSettings as? ImapServerSettings)
        val outgoingServer = requireNotNull(settings.outgoingServerSettings as? SmtpServerSettings)

        echo("------------------------------")
        echo("Source: ${settings.source}")
        echo("")
        echo("Incoming server:")
        echo("  Hostname:            ${incomingServer.hostname.value}")
        echo("  Port:                ${incomingServer.port.value}")
        echo("  Connection security: ${incomingServer.connectionSecurity}")
        echo("  Authentication:      ${incomingServer.authenticationTypes.joinToString()}")
        echo("  Username:            ${incomingServer.username}")
        echo("")
        echo("Outgoing server:")
        echo("  Hostname:            ${outgoingServer.hostname.value}")
        echo("  Port:                ${outgoingServer.port.value}")
        echo("  Connection security: ${outgoingServer.connectionSecurity}")
        echo("  Authentication:      ${outgoingServer.authenticationTypes.joinToString()}")
        echo("  Username:            ${outgoingServer.username}")
        echo("------------------------------")
        if (settings.isTrusted) {
            echo("These settings have been retrieved through trusted channels.")
        } else {
            echo("At least one UNTRUSTED channel was involved in retrieving these settings.")
        }
    }
}
