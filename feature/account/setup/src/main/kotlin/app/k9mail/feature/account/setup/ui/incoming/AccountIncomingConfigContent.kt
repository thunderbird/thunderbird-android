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
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.ui.common.defaultItemPadding
import app.k9mail.feature.account.setup.ui.toResourceString
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun AccountIncomingConfigContent(
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
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            item {
                Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
            }

            item {
                SelectInput(
                    options = IncomingProtocolType.all(),
                    selectedOption = IncomingProtocolType.DEFAULT,
                    onOptionChange = { },
                    label = stringResource(id = R.string.account_setup_incoming_config_protocol_type_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                TextInput(
                    text = "",
                    onTextChange = { },
                    label = stringResource(id = R.string.account_setup_incoming_config_server_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                SelectInput(
                    options = ConnectionSecurity.all(),
                    optionToStringTransformation = { it.toResourceString(resources) },
                    selectedOption = ConnectionSecurity.DEFAULT,
                    onOptionChange = { },
                    label = stringResource(id = R.string.account_setup_incoming_config_security_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                NumberInput(
                    value = null,
                    onValueChange = { },
                    label = stringResource(id = R.string.account_setup_outgoing_config_port_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                TextInput(
                    text = "",
                    onTextChange = { },
                    label = stringResource(id = R.string.account_setup_outgoing_config_username_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                PasswordInput(
                    password = "",
                    onPasswordChange = { },
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                // TODO add client certificate support
                SelectInput(
                    options = persistentListOf(
                        stringResource(
                            id = R.string.account_setup_client_certificate_none_available,
                        ),
                    ),
                    optionToStringTransformation = { it },
                    selectedOption = stringResource(
                        id = R.string.account_setup_client_certificate_none_available,
                    ),
                    onOptionChange = { },
                    label = stringResource(id = R.string.account_setup_outgoing_config_client_certificate_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                CheckboxInput(
                    text = stringResource(id = R.string.account_setup_incoming_config_imap_namespace_label),
                    checked = true,
                    onCheckedChange = { },
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                TextInput(
                    text = "",
                    onTextChange = { },
                    label = stringResource(id = R.string.account_setup_incoming_config_imap_prefix_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                CheckboxInput(
                    text = stringResource(id = R.string.account_setup_incoming_config_compression_label),
                    checked = true,
                    onCheckedChange = { },
                    contentPadding = defaultItemPadding(),
                )
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentK9Preview() {
    K9Theme {
        AccountIncomingConfigContent(
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountIncomingConfigContent(
            contentPadding = PaddingValues(),
        )
    }
}
