package app.k9mail.feature.account.setup.ui.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.item.ErrorItem
import app.k9mail.feature.account.common.ui.item.LoadingItem
import app.k9mail.feature.account.common.ui.item.SuccessItem
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.ui.clientcertificate.ClientCertificateInput
import app.k9mail.feature.account.setup.ui.common.mapper.toResourceString
import app.k9mail.feature.account.setup.ui.common.toResourceString
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State

@Suppress("LongMethod")
@Composable
internal fun AccountIncomingConfigContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("AccountIncomingConfigContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = if (state.isLoading || state.error != null || state.isSuccess) {
                Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically)
            } else {
                Arrangement.spacedBy(MainTheme.spacings.default)
            },
        ) {
            if (state.isLoading) {
                item(key = "loading") {
                    LoadingItem(
                        message = stringResource(id = R.string.account_setup_incoming_config_loading_message),
                    )
                }
            } else if (state.error != null) {
                item(key = "error") {
                    // TODO add raw error message
                    ErrorItem(
                        title = stringResource(id = R.string.account_setup_incoming_config_loading_error),
                        message = state.error.toResourceString(resources),
                        onRetry = { onEvent(Event.OnRetryClicked) },
                    )
                }
            } else if (state.isSuccess) {
                item(key = "success") {
                    SuccessItem(
                        message = stringResource(id = R.string.account_setup_incoming_config_success),
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
                }

                item {
                    SelectInput(
                        options = IncomingProtocolType.all(),
                        selectedOption = state.protocolType,
                        onOptionChange = { onEvent(Event.ProtocolTypeChanged(it)) },
                        label = stringResource(id = R.string.account_setup_incoming_config_protocol_type_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                item {
                    TextInput(
                        text = state.server.value,
                        errorMessage = state.server.error?.toResourceString(resources),
                        onTextChange = { onEvent(Event.ServerChanged(it)) },
                        label = stringResource(id = R.string.account_setup_incoming_config_server_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                item {
                    SelectInput(
                        options = ConnectionSecurity.all(),
                        optionToStringTransformation = { it.toResourceString(resources) },
                        selectedOption = state.security,
                        onOptionChange = { onEvent(Event.SecurityChanged(it)) },
                        label = stringResource(id = R.string.account_setup_incoming_config_security_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                item {
                    NumberInput(
                        value = state.port.value,
                        errorMessage = state.port.error?.toResourceString(resources),
                        onValueChange = { onEvent(Event.PortChanged(it)) },
                        label = stringResource(id = R.string.account_setup_outgoing_config_port_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                item {
                    SelectInput(
                        options = state.allowedAuthenticationTypes,
                        optionToStringTransformation = { it.toResourceString(resources) },
                        selectedOption = state.authenticationType,
                        onOptionChange = { onEvent(Event.AuthenticationTypeChanged(it)) },
                        label = stringResource(id = R.string.account_setup_incoming_config_authentication_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                item {
                    TextInput(
                        text = state.username.value,
                        errorMessage = state.username.error?.toResourceString(resources),
                        onTextChange = { onEvent(Event.UsernameChanged(it)) },
                        label = stringResource(id = R.string.account_setup_outgoing_config_username_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                if (state.isPasswordFieldVisible) {
                    item {
                        PasswordInput(
                            password = state.password.value,
                            errorMessage = state.password.error?.toResourceString(resources),
                            onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                            contentPadding = defaultItemPadding(),
                        )
                    }
                }

                item {
                    ClientCertificateInput(
                        alias = state.clientCertificateAlias,
                        onValueChange = { onEvent(Event.ClientCertificateChanged(it)) },
                        label = stringResource(id = R.string.account_setup_client_certificate_label),
                        contentPadding = defaultItemPadding(),
                    )
                }

                if (state.protocolType == IncomingProtocolType.IMAP) {
                    item {
                        CheckboxInput(
                            text = stringResource(id = R.string.account_setup_incoming_config_imap_namespace_label),
                            checked = state.imapAutodetectNamespaceEnabled,
                            onCheckedChange = { onEvent(Event.ImapAutoDetectNamespaceChanged(it)) },
                            contentPadding = defaultItemPadding(),
                        )
                    }

                    item {
                        if (state.imapAutodetectNamespaceEnabled) {
                            TextInput(
                                onTextChange = {},
                                label = stringResource(id = R.string.account_setup_incoming_config_imap_prefix_label),
                                contentPadding = defaultItemPadding(),
                                isEnabled = false,
                            )
                        } else {
                            TextInput(
                                text = state.imapPrefix.value,
                                errorMessage = state.imapPrefix.error?.toResourceString(resources),
                                onTextChange = { onEvent(Event.ImapPrefixChanged(it)) },
                                label = stringResource(id = R.string.account_setup_incoming_config_imap_prefix_label),
                                contentPadding = defaultItemPadding(),
                            )
                        }
                    }

                    item {
                        CheckboxInput(
                            text = stringResource(id = R.string.account_setup_incoming_config_imap_compression_label),
                            checked = state.imapUseCompression,
                            onCheckedChange = { onEvent(Event.ImapUseCompressionChanged(it)) },
                            contentPadding = defaultItemPadding(),
                        )
                    }

                    item {
                        CheckboxInput(
                            text = stringResource(R.string.account_setup_incoming_config_imap_send_client_id_label),
                            checked = state.imapSendClientId,
                            onCheckedChange = { onEvent(Event.ImapSendClientIdChanged(it)) },
                            contentPadding = defaultItemPadding(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentK9Preview() {
    K9Theme {
        AccountIncomingConfigContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountIncomingConfigContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}
