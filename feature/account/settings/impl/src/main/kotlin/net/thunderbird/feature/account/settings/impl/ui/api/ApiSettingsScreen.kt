package net.thunderbird.feature.account.settings.impl.ui.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.thunderbird.components.ui.bolt.atom.button.ButtonOutlined
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError
import net.thunderbird.feature.account.settings.impl.ui.api.ApiSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.api.ApiSettingsContract.Event
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ApiSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    onReauthorize: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApiSettingsContract.ViewModel = koinViewModel<ApiSettingsViewModel> {
        parametersOf(accountId)
    },
) {
    val graphReauthorizationEnabled = LocalContext.current.resources.getBoolean(
        R.bool.account_settings_graph_reauthorization_enabled,
    )
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.ReauthorizeAccount -> onReauthorize()
        }
    }

    BackHandler { dispatch(Event.OnBackPressed) }
    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = stringResource(R.string.account_settings_api_title),
                onBackClick = { dispatch(Event.OnBackPressed) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (state.value.isLoading) {
            LoadingView(
                message = stringResource(R.string.account_settings_api_title),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            ApiSettingsForm(
                state = state.value,
                graphReauthorizationEnabled = graphReauthorizationEnabled,
                modifier = Modifier.padding(innerPadding),
                dispatch = dispatch,
            )
        }
    }
}

@Composable
private fun ApiSettingsForm(
    state: ApiSettingsContract.State,
    graphReauthorizationEnabled: Boolean,
    modifier: Modifier = Modifier,
    dispatch: (Event) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BoltTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
    ) {
        ApiSettingsNameInput(state, dispatch)
        ApiSettingsEmailInput(state)
        if (state.requiresClientId) ApiSettingsClientIdInput(state, dispatch)
        ApiSettingsReauthorizationButton(state, graphReauthorizationEnabled, dispatch)
    }
}

@Composable
private fun ApiSettingsNameInput(state: ApiSettingsContract.State, dispatch: (Event) -> Unit) {
    TextInput(
        text = state.name.value,
        onTextChange = { dispatch(Event.OnNameChange(it)) },
        label = stringResource(R.string.account_settings_api_name_label),
        isRequired = true,
        errorMessage = state.name.error?.let { error ->
            when (error) {
                ValidateAccountNameError.EmptyName ->
                    stringResource(R.string.account_settings_general_name_error_empty)

                ValidateAccountNameError.TooLongName ->
                    stringResource(R.string.account_settings_general_name_error_too_long)

                else -> null
            }
        },
    )
}

@Composable
private fun ApiSettingsEmailInput(state: ApiSettingsContract.State) {
    TextInput(
        text = state.emailId,
        onTextChange = {},
        label = stringResource(R.string.account_settings_api_email_id_label),
        isRequired = true,
        errorMessage = if (state.emailId.isBlank()) {
            stringResource(R.string.account_settings_api_email_required)
        } else {
            null
        },
        isEnabled = false,
    )
}

@Composable
private fun ApiSettingsClientIdInput(state: ApiSettingsContract.State, dispatch: (Event) -> Unit) {
    TextInput(
        text = state.clientId,
        onTextChange = { dispatch(Event.OnClientIdChange(it)) },
        label = stringResource(R.string.account_settings_api_client_id_label),
        isRequired = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        errorMessage = if (state.clientId.isBlank()) {
            stringResource(R.string.account_settings_api_client_id_required)
        } else {
            null
        },
    )
}

@Composable
private fun ApiSettingsReauthorizationButton(
    state: ApiSettingsContract.State,
    graphReauthorizationEnabled: Boolean,
    dispatch: (Event) -> Unit,
) {
    val accountCanBeReauthorized = state.isApiAccount && state.emailId.isNotBlank()
    val graphIsAllowed = !state.isGraphAccount || graphReauthorizationEnabled
    val clientIdIsReady = !state.requiresClientId || state.clientId.isNotBlank()
    ButtonOutlined(
        text = stringResource(R.string.account_settings_api_reauthorize),
        onClick = { dispatch(Event.OnReauthorizeClick) },
        modifier = Modifier.fillMaxWidth(),
        enabled = accountCanBeReauthorized && graphIsAllowed && clientIdIsReady,
    )
}
