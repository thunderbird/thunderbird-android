package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonDefaults
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
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
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.brandBackground

@Composable
internal fun AccountAutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    brandName: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    ResponsiveWidthContainer(
        modifier = modifier
            .fillMaxSize()
            .brandBackground()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .imePadding()
            .testTagAsResourceId("AccountAutoDiscoveryContent"),
    ) { responsiveWidthPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(responsiveWidthPadding),
        ) {
            Spacer(modifier = Modifier.weight(weight = .15f))
            AppTitleTopHeader(title = brandName)
            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
            TextTitleLarge(
                text = stringResource(R.string.account_setup_discovery_add_email_account),
                color = MainTheme.colors.primary,
                modifier = Modifier.padding(
                    horizontal = MainTheme.spacings.double,
                    vertical = MainTheme.spacings.default,
                ),
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MainTheme.spacings.double),
            ) {
                AutoDiscoveryContent(
                    state = state,
                    onEvent = onEvent,
                    oAuthViewModel = oAuthViewModel,
                )
            }

            WizardNavigationBar(
                onNextClick = { onEvent(Event.OnNextClicked) },
                onBackClick = { onEvent(Event.OnBackClicked) },
                state = WizardNavigationBarState(showNext = state.isNextButtonVisible),
                modifier = Modifier.padding(horizontal = MainTheme.spacings.double),
            )
        }
    }
}

@Composable
internal fun AutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
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
                oAuthViewModel = oAuthViewModel,
            )
        },
        modifier = modifier
            .fillMaxSize(),
    )
}

@Composable
internal fun ContentView(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    Column(
        modifier = modifier.fillMaxSize(),
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

        EmailAddressInput(
            emailAddress = state.emailAddress.value,
            errorMessage = state.emailAddress.error?.toAutoDiscoveryValidationErrorString(resources),
            onEmailAddressChange = { onEvent(Event.EmailAddressChanged(it)) },
            contentPadding = PaddingValues(),
            modifier = Modifier.testTagAsResourceId("account_setup_email_address_input"),
        )

        if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.PASSWORD) {
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            PasswordInput(
                password = state.password.value,
                errorMessage = state.password.error?.toAutoDiscoveryValidationErrorString(resources),
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                contentPadding = PaddingValues(),
                modifier = Modifier.testTagAsResourceId("account_setup_password_input"),
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
        Spacer(Modifier.height(MainTheme.spacings.quadruple))

        AnimatedVisibility(
            visible = state.configStep == AccountAutoDiscoveryContract.ConfigStep.EMAIL_ADDRESS,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = MainTheme.spacings.quadruple),
            ) {
                TextTitleMedium(
                    text = stringResource(R.string.account_setup_discovery_migration),
                    color = MainTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 292.dp),
                )
                ButtonOutlined(
                    text = stringResource(R.string.account_setup_discovery_import_existing_account),
                    onClick = { onEvent(Event.ImportAccountClicked) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MainTheme.colors.primary),
                    shape = ButtonDefaults.outlinedShape(
                        border = ButtonDefaults.outlinedButtonBorder(color = MainTheme.colors.outline),
                    ),
                )
            }
        }
    }
}
