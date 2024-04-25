package app.k9mail.feature.account.server.settings.ui.outgoing.content

import android.content.res.Resources
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.theme2.MainTheme
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
            keyboardOptions = KeyboardOptions(autoCorrect = false),
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
                keyboardOptions = KeyboardOptions(autoCorrect = false),
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
}
