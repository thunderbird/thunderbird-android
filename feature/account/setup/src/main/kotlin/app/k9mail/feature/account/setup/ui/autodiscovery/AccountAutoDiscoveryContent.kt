package app.k9mail.feature.account.setup.ui.autodiscovery

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.textfield.nonLearningKeyboardOptions
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryResultApprovalView
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryResultView
import com.fsck.k9.mail.MailProxyType
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.component.ThundermailButtonPanel

@Composable
internal fun AccountAutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
    onThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    brandName: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    ResponsiveWidthContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .imePadding()
            .testTag("AccountAutoDiscoveryContent"),
    ) { responsiveWidthPadding ->
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(responsiveWidthPadding),
            ) {
                AppTitleTopHeader(
                    title = brandName,
                )
                Spacer(modifier = Modifier.weight(1f))
                @Suppress("ViewModelForwarding")
                AutoDiscoveryContent(
                    state = state,
                    onEvent = onEvent,
                    onThundermailClick = onThundermailClick,
                    onScanQrCodeClick = onScanQrCodeClick,
                    oAuthViewModel = oAuthViewModel,
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            WizardNavigationBar(
                onNextClick = { onEvent(Event.OnNextClicked) },
                onBackClick = { onEvent(Event.OnBackClicked) },
                state = WizardNavigationBarState(showNext = state.isNextButtonVisible),
            )
        }
    }
}

@Composable
internal fun AutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
    onThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    ContentLoadingErrorView(
        state = state,
        loading = {
            LoadingView(
                message = stringResource(id = R.string.account_setup_auto_discovery_loading_message),
                modifier = Modifier.fillMaxSize(),
            )
        },
        error = { error ->
            ErrorView(
                title = stringResource(id = R.string.account_setup_auto_discovery_loading_error),
                message = error.toAutoDiscoveryErrorString(resources),
                onRetry = { onEvent(Event.OnRetryClicked) },
                modifier = Modifier.fillMaxSize(),
            )
        },
        content = { contentState ->
            @Suppress("ViewModelForwarding")
            ContentView(
                state = contentState,
                onEvent = onEvent,
                onThundermailClick = onThundermailClick,
                onScanQrCodeClick = onScanQrCodeClick,
                oAuthViewModel = oAuthViewModel,
                resources = resources,
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    )
}

@Composable
internal fun ContentView(
    state: State,
    onEvent: (Event) -> Unit,
    onThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    resources: Resources,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MainTheme.spacings.quadruple)
            .then(modifier),
    ) {
        if (state.configStep != AccountAutoDiscoveryContract.ConfigStep.EMAIL_ADDRESS) {
            AutoDiscoveryResultView(
                settings = state.autoDiscoverySettings,
                onEditConfigurationClick = { onEvent(Event.OnEditConfigurationClicked) },
            )
            if (state.autoDiscoverySettings != null && state.autoDiscoverySettings.isTrusted.not()) {
                AutoDiscoveryResultApprovalView(
                    approvalState = state.configurationApproved,
                    onApprovalChange = { onEvent(Event.ResultApprovalChanged(it)) },
                )
            }
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
        }

        AnimatedVisibility(state.emailAddress.value.isBlank()) {
            ThundermailButtonPanel(
                onThundermailClick = onThundermailClick,
                onScanQrCodeClick = onScanQrCodeClick,
                modifier = Modifier
                    .testTag("thundermail_panel")
                    .padding(bottom = MainTheme.spacings.quadruple),
            )
        }

        EmailAddressInput(
            emailAddress = state.emailAddress.value,
            errorMessage = state.emailAddress.error?.toAutoDiscoveryValidationErrorString(resources),
            onEmailAddressChange = { onEvent(Event.EmailAddressChanged(it)) },
            contentPadding = PaddingValues(),
            modifier = Modifier.testTag("account_setup_email_address_input"),
        )

        if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.EMAIL_ADDRESS) {
            EmailStepActionButtons(
                onManualSetupClick = { onEvent(Event.OnManualSetupClicked) },
                onProxyToggleClick = { onEvent(Event.NetworkSettingsToggled) },
            )
            AnimatedVisibility(state.isNetworkSettingsExpanded) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
                NetworkSettingsView(
                    state = state,
                    onEvent = onEvent,
                    resources = resources,
                )
            }
        }

        if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.PASSWORD) {
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            PasswordInput(
                password = state.password.value,
                errorMessage = state.password.error?.toAutoDiscoveryValidationErrorString(resources),
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                contentPadding = PaddingValues(),
                usePrivateKeyboard = state.isPrivateKeyboardEnabled,
                modifier = Modifier.testTag("account_setup_password_input"),
            )
        } else if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.OAUTH) {
            val isAutoDiscoverySettingsTrusted = state.autoDiscoverySettings?.isTrusted ?: false
            val isConfigurationApproved = state.configurationApproved.value ?: false
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            AccountOAuthView(
                onOAuthResult = { result -> onEvent(Event.OnOAuthResult(result)) },
                viewModel = oAuthViewModel,
                isEnabled = isAutoDiscoverySettingsTrusted || isConfigurationApproved,
            )
        }
    }
}

@Composable
private fun EmailStepActionButtons(
    onManualSetupClick: () -> Unit,
    onProxyToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_setup_email_step_action_buttons"),
        horizontalArrangement = Arrangement.End,
    ) {
        ButtonText(
            text = stringResource(id = R.string.account_setup_auto_discovery_manual_setup_button_label),
            onClick = onManualSetupClick,
            modifier = Modifier.testTag("account_setup_manual_setup_button"),
        )
        ButtonText(
            text = stringResource(id = R.string.account_setup_auto_discovery_proxy_button_label),
            onClick = onProxyToggleClick,
            modifier = Modifier.testTag("account_setup_proxy_toggle_button"),
        )
    }
}

@Composable
private fun NetworkSettingsView(
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("account_setup_network_settings"),
    ) {
        TextTitleMedium(text = stringResource(id = R.string.account_setup_auto_discovery_network_settings_title))
        TextBodyMedium(text = stringResource(id = R.string.account_setup_auto_discovery_network_settings_summary))
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))

        SelectInput(
            options = listOf(
                MailProxyType.USE_GLOBAL,
                MailProxyType.NONE,
                MailProxyType.HTTP,
                MailProxyType.SOCKS4,
                MailProxyType.SOCKS5,
            ).toImmutableList(),
            optionToStringTransformation = { it.toAutoDiscoveryProxyTypeString(resources) },
            selectedOption = state.proxyType,
            onOptionChange = { onEvent(Event.ProxyTypeChanged(it)) },
            label = stringResource(id = R.string.account_setup_auto_discovery_proxy_type_label),
            contentPadding = PaddingValues(),
        )

        if (state.proxyType.isSetupProxyConfigured) {
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            TextInput(
                text = state.proxyServer.value,
                errorMessage = state.proxyServer.error?.toAutoDiscoveryValidationErrorString(resources),
                onTextChange = { onEvent(Event.ProxyServerChanged(it)) },
                label = stringResource(id = R.string.account_setup_auto_discovery_proxy_server_label),
                isRequired = true,
                contentPadding = PaddingValues(),
                keyboardOptions = nonLearningKeyboardOptions(state.isPrivateKeyboardEnabled),
            )

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            NumberInput(
                value = state.proxyPort.value,
                errorMessage = state.proxyPort.error?.toAutoDiscoveryValidationErrorString(resources),
                onValueChange = { onEvent(Event.ProxyPortChanged(it)) },
                label = stringResource(id = R.string.account_setup_auto_discovery_proxy_port_label),
                isRequired = true,
                contentPadding = PaddingValues(),
            )

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            TextInput(
                text = state.proxyUsername.value,
                errorMessage = state.proxyUsername.error?.toAutoDiscoveryValidationErrorString(resources),
                onTextChange = { onEvent(Event.ProxyUsernameChanged(it)) },
                label = stringResource(id = R.string.account_setup_auto_discovery_proxy_username_label),
                contentPadding = PaddingValues(),
                keyboardOptions = nonLearningKeyboardOptions(state.isPrivateKeyboardEnabled),
                contentType = ContentType.Username,
            )

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            PasswordInput(
                password = state.proxyPassword.value,
                label = stringResource(id = R.string.account_setup_auto_discovery_proxy_password_label),
                errorMessage = state.proxyPassword.error?.toAutoDiscoveryValidationErrorString(resources),
                onPasswordChange = { onEvent(Event.ProxyPasswordChanged(it)) },
                contentPadding = PaddingValues(),
                usePrivateKeyboard = state.isPrivateKeyboardEnabled,
            )

            if (state.proxyType.supportsProxyDns) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                CheckboxInput(
                    text = stringResource(id = R.string.account_setup_auto_discovery_proxy_dns_label),
                    checked = state.proxyDns,
                    onCheckedChange = { onEvent(Event.ProxyDnsChanged(it)) },
                    contentPadding = PaddingValues(),
                )
            }
        }
    }
}

private fun MailProxyType.toAutoDiscoveryProxyTypeString(resources: Resources): String {
    return when (this) {
        MailProxyType.USE_GLOBAL -> resources.getString(R.string.account_setup_auto_discovery_proxy_type_use_global)
        MailProxyType.NONE -> resources.getString(R.string.account_setup_auto_discovery_proxy_type_none)
        MailProxyType.HTTP -> resources.getString(R.string.account_setup_auto_discovery_proxy_type_http)
        MailProxyType.SOCKS4 -> resources.getString(R.string.account_setup_auto_discovery_proxy_type_socks4)
        MailProxyType.SOCKS5 -> resources.getString(R.string.account_setup_auto_discovery_proxy_type_socks5)
    }
}

private val MailProxyType.isSetupProxyConfigured: Boolean
    get() = this != MailProxyType.USE_GLOBAL && this != MailProxyType.NONE

private val MailProxyType.supportsProxyDns: Boolean
    get() = this == MailProxyType.SOCKS4 || this == MailProxyType.SOCKS5
