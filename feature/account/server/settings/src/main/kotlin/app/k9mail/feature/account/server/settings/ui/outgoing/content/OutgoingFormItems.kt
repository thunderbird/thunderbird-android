package app.k9mail.feature.account.server.settings.ui.outgoing.content

import android.content.res.Resources
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.common.ClientCertificateInput
import app.k9mail.feature.account.server.settings.ui.common.ServerSettingsPasswordInput
import app.k9mail.feature.account.server.settings.ui.common.mapper.toResourceString
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.outgoing.isPasswordFieldVisible
import app.k9mail.feature.account.server.settings.ui.outgoing.isUsernameFieldVisible
import com.fsck.k9.mail.MailProxyType
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Suppress("LongMethod")
internal fun LazyListScope.outgoingFormItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    item {
        Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
    }

    item {
        TextInput(
            text = state.server.value,
            errorMessage = state.server.error?.toResourceString(resources),
            onTextChange = { onEvent(Event.ServerChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_server_label),
            isRequired = true,
            contentPadding = defaultItemPadding(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        )
    }

    item {
        SelectInput(
            options = ConnectionSecurity.all(),
            optionToStringTransformation = { it.toResourceString(resources) },
            selectedOption = state.security,
            onOptionChange = { onEvent(Event.SecurityChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_security_label),
            contentPadding = defaultItemPadding(),
        )
    }

    item {
        NumberInput(
            value = state.port.value,
            errorMessage = state.port.error?.toResourceString(resources),
            onValueChange = { onEvent(Event.PortChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_port_label),
            isRequired = true,
            contentPadding = defaultItemPadding(),
        )
    }

    item {
        SelectInput(
            options = AuthenticationType.outgoing(),
            optionToStringTransformation = { it.toResourceString(resources) },
            selectedOption = state.authenticationType,
            onOptionChange = { onEvent(Event.AuthenticationTypeChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_authentication_label),
            contentPadding = defaultItemPadding(),
        )
    }

    if (state.isUsernameFieldVisible) {
        item {
            TextInput(
                text = state.username.value,
                errorMessage = state.username.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.UsernameChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_username_label),
                isRequired = true,
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                contentType = ContentType.Username + ContentType.EmailAddress,
            )
        }
    }

    if (state.isPasswordFieldVisible) {
        item {
            ServerSettingsPasswordInput(
                mode = mode,
                password = state.password.value,
                errorMessage = state.password.error?.toResourceString(resources),
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                isRequired = true,
                contentPadding = defaultItemPadding(),
            )
        }
    }

    item {
        ClientCertificateInput(
            alias = state.clientCertificateAlias,
            onValueChange = { onEvent(Event.ClientCertificateChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_client_certificate_label),
            contentPadding = defaultItemPadding(),
        )
    }

    proxyFormItems(state, onEvent, resources)

    item {
        Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
    }
}

private fun LazyListScope.proxyFormItems(
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    item {
        SelectInput(
            options = listOf(MailProxyType.NONE, MailProxyType.HTTP, MailProxyType.SOCKS).toImmutableList(),
            optionToStringTransformation = { it.toResourceString(resources) },
            selectedOption = state.proxyType,
            onOptionChange = { onEvent(Event.ProxyTypeChanged(it)) },
            label = stringResource(id = R.string.account_server_settings_proxy_type_label),
            contentPadding = defaultItemPadding(),
        )
    }

    if (state.proxyType != MailProxyType.NONE) {
        item {
            TextInput(
                text = state.proxyServer.value,
                errorMessage = state.proxyServer.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.ProxyServerChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_proxy_server_label),
                isRequired = true,
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
        }

        item {
            NumberInput(
                value = state.proxyPort.value,
                errorMessage = state.proxyPort.error?.toResourceString(resources),
                onValueChange = { onEvent(Event.ProxyPortChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_proxy_port_label),
                isRequired = true,
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            CheckboxInput(
                text = stringResource(id = R.string.account_server_settings_proxy_dns_label),
                checked = state.proxyDns,
                onCheckedChange = { onEvent(Event.ProxyDnsChanged(it)) },
                contentPadding = defaultItemPadding(),
            )
        }
    }
}
