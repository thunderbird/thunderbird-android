package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.setup.R

@Composable
internal fun AutoDiscoveryResultBodyView(
    settings: AutoDiscoveryResult.Settings,
    onEditConfigurationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MainTheme.spacings.default)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        if (settings.isTrusted.not()) {
            Spacer(modifier = Modifier.height(MainTheme.sizes.smaller))
            TextBodyMedium(
                text = stringResource(
                    id = R.string.account_setup_auto_discovery_result_disclaimer_untrusted_configuration,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val incomingServerSettings = settings.incomingServerSettings
        if (incomingServerSettings is ImapServerSettings) {
            Spacer(modifier = Modifier.height(MainTheme.sizes.smaller))
            AutoDiscoveryServerSettingsView(
                protocolName = "IMAP",
                serverHostname = incomingServerSettings.hostname,
                serverPort = incomingServerSettings.port.value,
                connectionSecurity = incomingServerSettings.connectionSecurity,
                username = incomingServerSettings.username,
                isIncoming = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val outgoingServerSettings = settings.outgoingServerSettings
        if (outgoingServerSettings is SmtpServerSettings) {
            Spacer(modifier = Modifier.height(MainTheme.sizes.smaller))
            AutoDiscoveryServerSettingsView(
                protocolName = "SMTP",
                serverHostname = outgoingServerSettings.hostname,
                serverPort = outgoingServerSettings.port.value,
                connectionSecurity = outgoingServerSettings.connectionSecurity,
                username = outgoingServerSettings.username,
                isIncoming = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        EditConfigurationButton(
            onEditConfigurationClick = onEditConfigurationClick,
        )
    }
}

@Composable
internal fun EditConfigurationButton(
    modifier: Modifier = Modifier,
    onEditConfigurationClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
    ) {
        ButtonText(
            text = stringResource(id = R.string.account_setup_auto_discovery_result_edit_configuration_button_label),
            onClick = onEditConfigurationClick,
            color = MainTheme.colors.warning,
        )
    }
}

@Preview
@Composable
internal fun AutoDiscoveryResultBodyViewPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultBodyView(
            settings = AutoDiscoveryResult.Settings(
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
                isTrusted = true,
                source = "preview",
            ),
            onEditConfigurationClick = {},
        )
    }
}
