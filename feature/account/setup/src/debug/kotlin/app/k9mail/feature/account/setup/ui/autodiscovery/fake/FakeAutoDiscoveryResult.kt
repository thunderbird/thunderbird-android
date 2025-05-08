package app.k9mail.feature.account.setup.ui.autodiscovery.fake

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import net.thunderbird.core.common.net.toHostname
import net.thunderbird.core.common.net.toPort

internal fun fakeAutoDiscoveryResultSettings(isTrusted: Boolean) =
    AutoDiscoveryResult.Settings(
        incomingServerSettings = ImapServerSettings(
            hostname = "imap.example.com".toHostname(),
            port = 993.toPort(),
            connectionSecurity = ConnectionSecurity.TLS,
            authenticationTypes = listOf(AuthenticationType.PasswordEncrypted),
            username = "",
        ),
        outgoingServerSettings = SmtpServerSettings(
            hostname = "smtp.example.com".toHostname(),
            port = 465.toPort(),
            connectionSecurity = ConnectionSecurity.TLS,
            authenticationTypes = listOf(AuthenticationType.PasswordEncrypted),
            username = "",
        ),
        isTrusted = isTrusted,
        source = "preview",
    )
