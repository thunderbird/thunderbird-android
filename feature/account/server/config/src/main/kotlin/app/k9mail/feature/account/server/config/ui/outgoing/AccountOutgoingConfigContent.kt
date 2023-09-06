package app.k9mail.feature.account.server.config.ui.outgoing

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
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.server.config.R
import app.k9mail.feature.account.server.config.ui.common.ClientCertificateInput
import app.k9mail.feature.account.server.config.ui.common.mapper.toResourceString
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract.State

@Suppress("LongMethod")
@Composable
internal fun AccountOutgoingConfigContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("AccountOutgoingConfigContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            item {
                Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
            }

            item {
                TextInput(
                    text = state.server.value,
                    errorMessage = state.server.error?.toResourceString(resources),
                    onTextChange = { onEvent(Event.ServerChanged(it)) },
                    label = stringResource(id = R.string.account_server_config_server_label),
                    isRequired = true,
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                SelectInput(
                    options = ConnectionSecurity.all(),
                    optionToStringTransformation = { it.toResourceString(resources) },
                    selectedOption = state.security,
                    onOptionChange = { onEvent(Event.SecurityChanged(it)) },
                    label = stringResource(id = R.string.account_server_config_security_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                NumberInput(
                    value = state.port.value,
                    errorMessage = state.port.error?.toResourceString(resources),
                    onValueChange = { onEvent(Event.PortChanged(it)) },
                    label = stringResource(id = R.string.account_server_config_port_label),
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
                    label = stringResource(id = R.string.account_server_config_authentication_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            if (state.isUsernameFieldVisible) {
                item {
                    TextInput(
                        text = state.username.value,
                        errorMessage = state.username.error?.toResourceString(resources),
                        onTextChange = { onEvent(Event.UsernameChanged(it)) },
                        label = stringResource(id = R.string.account_server_config_username_label),
                        isRequired = true,
                        contentPadding = defaultItemPadding(),
                    )
                }
            }

            if (state.isPasswordFieldVisible) {
                item {
                    PasswordInput(
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
                    label = stringResource(id = R.string.account_server_config_client_certificate_label),
                    contentPadding = defaultItemPadding(),
                )
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingConfigContentK9Preview() {
    K9Theme {
        AccountOutgoingConfigContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOutgoingConfigContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}
