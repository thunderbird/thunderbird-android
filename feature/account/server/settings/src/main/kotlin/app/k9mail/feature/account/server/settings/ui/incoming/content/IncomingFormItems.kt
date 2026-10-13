package app.k9mail.feature.account.server.settings.ui.incoming.content

import android.content.res.Resources
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.isNativeRestApi
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.common.ClientCertificateInput
import app.k9mail.feature.account.server.settings.ui.common.ServerSettingsPasswordInput
import app.k9mail.feature.account.server.settings.ui.common.mapper.toResourceString
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.allowedAuthenticationTypes
import app.k9mail.feature.account.server.settings.ui.incoming.isPasswordFieldVisible
import net.thunderbird.components.ui.bolt.molecule.input.CheckboxInput
import net.thunderbird.components.ui.bolt.molecule.input.NumberInput
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.theme.BoltTheme

internal fun LazyListScope.incomingFormItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
    applicationId: String,
) {
    item { Spacer(modifier = Modifier.requiredHeight(BoltTheme.sizes.smaller)) }
    protocolTypeItem(mode, state, onEvent, resources)
    apiOAuthConfigurationItems(mode, state, onEvent, resources, applicationId)
    serverConnectionItems(state, onEvent, resources)
    credentialsItems(mode, state, onEvent, resources)
    clientCertificateItems(state, onEvent)
    protocolSpecificItems(mode, state, onEvent, resources, applicationId)
    item { Spacer(modifier = Modifier.requiredHeight(BoltTheme.sizes.smaller)) }
}

private fun LazyListScope.protocolTypeItem(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    if (mode == InteractionMode.Create) {
        item {
            SelectInput(
                options = IncomingProtocolType.all(),
                optionToStringTransformation = {
                    when (it) {
                        IncomingProtocolType.GRAPH -> resources.getString(
                            R.string.account_server_settings_protocol_type_graph,
                        )

                        IncomingProtocolType.GMAIL_API ->
                            resources.getString(R.string.account_server_settings_protocol_type_gmail_api)

                        IncomingProtocolType.ZOHO_MAIL_API ->
                            resources.getString(R.string.account_server_settings_protocol_type_zoho_api)

                        else -> it.name
                    }
                },
                selectedOption = state.protocolType,
                onOptionChange = { onEvent(Event.ProtocolTypeChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_connector_label),
                contentPadding = defaultItemPadding(),
            )
        }
    }
}

private fun LazyListScope.apiOAuthConfigurationItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
    applicationId: String,
) {
    val isGraphOrGmail = state.protocolType == IncomingProtocolType.GRAPH ||
        state.protocolType == IncomingProtocolType.GMAIL_API
    if (mode == InteractionMode.Create && isGraphOrGmail) {
        item {
            TextInput(
                text = state.apiOAuthClientId.value,
                errorMessage = if (state.apiConfigurationError && state.apiOAuthClientId.value.isBlank()) {
                    resources.getString(R.string.account_server_settings_api_client_id_required)
                } else {
                    null
                },
                onTextChange = { onEvent(Event.ApiOAuthClientIdChanged(it)) },
                label = stringResource(R.string.account_server_settings_api_client_id_label),
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
        }
        item {
            val redirectUri = state.apiOAuthRedirectUri.value
            val redirectUriError = when {
                !state.apiConfigurationError -> null

                redirectUri.isBlank() -> resources.getString(R.string.account_server_settings_api_redirect_required)

                applicationId.isNotBlank() && !isRegisteredOAuthRedirectUri(redirectUri, applicationId) ->
                    resources.getString(R.string.account_server_settings_api_redirect_invalid)

                else -> null
            }
            TextInput(
                text = redirectUri,
                errorMessage = redirectUriError,
                onTextChange = { onEvent(Event.ApiOAuthRedirectUriChanged(it)) },
                label = stringResource(R.string.account_server_settings_api_redirect_uri_label),
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
        }
    }
}

private fun LazyListScope.serverConnectionItems(state: State, onEvent: (Event) -> Unit, resources: Resources) {
    if (!state.protocolType.isNativeRestApi()) {
        item {
            TextInput(
                text = state.server.value,
                errorMessage = state.server.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.ServerChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_server_label),
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
                contentPadding = defaultItemPadding(),
            )
        }
        item {
            SelectInput(
                options = state.allowedAuthenticationTypes,
                optionToStringTransformation = { it.toResourceString(resources) },
                selectedOption = state.authenticationType,
                onOptionChange = { onEvent(Event.AuthenticationTypeChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_authentication_label),
                contentPadding = defaultItemPadding(),
            )
        }
    }
}

private fun LazyListScope.credentialsItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    if (!state.protocolType.isNativeRestApi()) {
        item {
            TextInput(
                text = state.username.value,
                errorMessage = state.username.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.UsernameChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_username_label),
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                contentType = ContentType.Username + ContentType.EmailAddress,
            )
        }
    }
    if (!state.protocolType.isNativeRestApi() && state.isPasswordFieldVisible) {
        item {
            ServerSettingsPasswordInput(
                mode = mode,
                password = state.password.value,
                errorMessage = state.password.error?.toResourceString(resources),
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                contentPadding = defaultItemPadding(),
            )
        }
    }
}

private fun LazyListScope.clientCertificateItems(state: State, onEvent: (Event) -> Unit) {
    if (!state.protocolType.isNativeRestApi()) {
        item {
            ClientCertificateInput(
                alias = state.clientCertificateAlias,
                onValueChange = { onEvent(Event.ClientCertificateChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_client_certificate_label),
                contentPadding = defaultItemPadding(),
            )
        }
    }
}

private fun LazyListScope.protocolSpecificItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
    applicationId: String,
) {
    if (state.protocolType == IncomingProtocolType.IMAP) imapFormItems(state, onEvent, resources)
    if (state.protocolType == IncomingProtocolType.ZOHO_MAIL_API) {
        zohoFormItems(mode, state, onEvent, resources, applicationId)
    }
}

private fun LazyListScope.zohoFormItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
    applicationId: String,
) {
    if (mode == InteractionMode.Create) {
        item {
            TextInput(
                text = state.username.value,
                errorMessage = if (state.zohoConfigurationError && state.username.value.isBlank()) {
                    resources.getString(R.string.account_server_settings_zoho_email_required)
                } else {
                    null
                },
                onTextChange = { onEvent(Event.UsernameChanged(it)) },
                label = stringResource(R.string.account_server_settings_zoho_email_label),
                contentPadding = defaultItemPadding(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                contentType = ContentType.EmailAddress,
            )
        }
    }

    zohoOAuthConfigurationItems(mode, state, onEvent, resources)
    item {
        val redirectUri = state.zohoRedirectUri.value
        val redirectUriError = when {
            !state.zohoConfigurationError -> null

            redirectUri.isBlank() -> resources.getString(R.string.account_server_settings_zoho_redirect_required)

            applicationId.isNotBlank() && !isRegisteredOAuthRedirectUri(redirectUri, applicationId) ->
                resources.getString(R.string.account_server_settings_zoho_redirect_invalid)

            else -> null
        }
        TextInput(
            text = redirectUri,
            errorMessage = redirectUriError,
            onTextChange = { onEvent(Event.ZohoRedirectUriChanged(it)) },
            label = stringResource(R.string.account_server_settings_zoho_redirect_uri_label),
            contentPadding = defaultItemPadding(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        )
    }
}

private fun LazyListScope.zohoOAuthConfigurationItems(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    item {
        TextInput(
            text = state.zohoClientId.value,
            errorMessage = if (state.zohoConfigurationError && state.zohoClientId.value.isBlank()) {
                resources.getString(R.string.account_server_settings_zoho_client_id_required)
            } else {
                null
            },
            onTextChange = { onEvent(Event.ZohoClientIdChanged(it)) },
            label = stringResource(R.string.account_server_settings_zoho_client_id_label),
            contentPadding = defaultItemPadding(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        )
    }

    item {
        ServerSettingsPasswordInput(
            mode = mode,
            password = state.password.value,
            onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
            label = stringResource(R.string.account_server_settings_zoho_client_secret_label),
            contentPadding = defaultItemPadding(),
        )
    }
}
