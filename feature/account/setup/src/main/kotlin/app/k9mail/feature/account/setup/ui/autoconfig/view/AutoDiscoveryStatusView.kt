package app.k9mail.feature.account.setup.ui.autoconfig.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
internal fun AutoDiscoveryStatusView(
    settings: AutoDiscoveryResult.Settings?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Surface(
            shape = MainTheme.shapes.small,
            modifier = Modifier
                .border(1.dp, Color.Gray.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(MainTheme.spacings.default),
            ) {
                AutoDiscoveryStatusHeaderView(
                    state = if (settings == null) {
                        AutoDiscoveryStatusHeaderState.NoSettings
                    } else if (settings.isTrusted) {
                        AutoDiscoveryStatusHeaderState.Trusted
                    } else {
                        AutoDiscoveryStatusHeaderState.Untrusted
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun AutoDiscoveryStatusViewTrustedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusView(
            settings = AutoDiscoveryResult.Settings(
                incomingServerSettings = ImapServerSettings(
                    hostname = "imap.example.com".toHostname(),
                    port = 993.toPort(),
                    connectionSecurity = ConnectionSecurity.TLS,
                    authenticationType = AuthenticationType.PasswordEncrypted,
                    username = "",
                ),
                outgoingServerSettings = SmtpServerSettings(
                    hostname = "smtp.example.com".toHostname(),
                    port = 465.toPort(),
                    connectionSecurity = ConnectionSecurity.TLS,
                    authenticationType = AuthenticationType.PasswordEncrypted,
                    username = "",
                ),
                isTrusted = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun AutoDiscoveryStatusViewUntrustedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusView(
            settings = AutoDiscoveryResult.Settings(
                incomingServerSettings = ImapServerSettings(
                    hostname = "imap.example.com".toHostname(),
                    port = 993.toPort(),
                    connectionSecurity = ConnectionSecurity.TLS,
                    authenticationType = AuthenticationType.PasswordEncrypted,
                    username = "",
                ),
                outgoingServerSettings = SmtpServerSettings(
                    hostname = "smtp.example.com".toHostname(),
                    port = 465.toPort(),
                    connectionSecurity = ConnectionSecurity.TLS,
                    authenticationType = AuthenticationType.PasswordEncrypted,
                    username = "",
                ),
                isTrusted = false,
            ),
        )
    }
}
