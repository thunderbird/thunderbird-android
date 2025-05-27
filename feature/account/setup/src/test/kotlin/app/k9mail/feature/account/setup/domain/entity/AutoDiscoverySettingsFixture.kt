package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import net.thunderbird.core.common.net.toHostname
import net.thunderbird.core.common.net.toPort

object AutoDiscoverySettingsFixture {

    val settings = AutoDiscoveryResult.Settings(
        incomingServerSettings = ImapServerSettings(
            hostname = "incoming.example.com".toHostname(),
            port = 123.toPort(),
            connectionSecurity = ConnectionSecurity.TLS,
            authenticationTypes = listOf(AuthenticationType.PasswordEncrypted),
            username = "incoming_username",
        ),
        outgoingServerSettings = SmtpServerSettings(
            hostname = "outgoing.example.com".toHostname(),
            port = 456.toPort(),
            connectionSecurity = ConnectionSecurity.TLS,
            authenticationTypes = listOf(AuthenticationType.PasswordEncrypted),
            username = "outgoing_username",
        ),
        isTrusted = true,
        source = "test",
    )
}
