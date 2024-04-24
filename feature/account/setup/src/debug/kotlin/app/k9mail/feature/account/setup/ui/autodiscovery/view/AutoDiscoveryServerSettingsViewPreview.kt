package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryServerSettingsViewPreview() {
    PreviewWithThemes {
        AutoDiscoveryServerSettingsView(
            protocolName = "IMAP",
            serverHostname = "imap.example.com".toHostname(),
            serverPort = 993,
            connectionSecurity = ConnectionSecurity.TLS,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryServerSettingsViewOutgoingPreview() {
    PreviewWithThemes {
        AutoDiscoveryServerSettingsView(
            protocolName = "IMAP",
            serverHostname = "imap.example.com".toHostname(),
            serverPort = 993,
            connectionSecurity = ConnectionSecurity.TLS,
            isIncoming = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryServerSettingsViewWithUserPreview() {
    PreviewWithThemes {
        AutoDiscoveryServerSettingsView(
            protocolName = "IMAP",
            serverHostname = "imap.example.com".toHostname(),
            serverPort = 993,
            connectionSecurity = ConnectionSecurity.TLS,
            username = "username",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryServerSettingsViewWithIpAddressPreview() {
    PreviewWithThemes {
        AutoDiscoveryServerSettingsView(
            protocolName = "IMAP",
            serverHostname = "127.0.0.1".toHostname(),
            serverPort = 993,
            connectionSecurity = ConnectionSecurity.TLS,
            username = "username",
        )
    }
}
