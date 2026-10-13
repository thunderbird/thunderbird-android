package app.k9mail.feature.account.edit.ui.server.settings.reauthorize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.edit.R
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationContract.Error
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationContract.Event
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationContract.Provider
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.contract.mvi.observe

@Composable
fun ApiAccountReauthorizationScreen(
    onBack: () -> Unit,
    viewModel: ApiAccountReauthorizationContract.ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe {}

    LaunchedEffect(Unit) {
        dispatch(Event.Load)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = stringResource(state.value.provider.titleResource()),
                onBackClick = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = BoltTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            if (state.value.isSuccess) {
                TextBodyMedium(text = stringResource(state.value.provider.successResource()))
                ButtonText(
                    text = stringResource(R.string.account_edit_api_reauthorization_done),
                    onClick = onBack,
                )
            } else {
                TextBodyMedium(text = stringResource(state.value.provider.explanationResource()))
                state.value.error?.let { error ->
                    TextBodyMedium(text = stringResource(error.messageResource(state.value.provider)))
                }

                if (state.value.isInitializing || state.value.isSaving) {
                    LoadingView(
                        message = stringResource(R.string.account_edit_api_reauthorization_loading),
                    )
                } else if (state.value.error != Error.NotAvailable) {
                    AccountOAuthView(
                        onOAuthResult = { dispatch(Event.OnOAuthResult(it)) },
                        viewModel = viewModel.accountOAuthViewModel,
                        isEnabled = !state.value.isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun Provider?.titleResource(): Int = when (this) {
    Provider.Google -> R.string.account_edit_gmail_reauthorization_title
    Provider.Microsoft -> R.string.account_edit_graph_reauthorization_title
    Provider.Zoho -> R.string.account_edit_zoho_reauthorization_title
    null -> R.string.account_edit_api_reauthorization_title
}

@Composable
private fun Provider?.explanationResource(): Int = when (this) {
    Provider.Google -> R.string.account_edit_gmail_reauthorization_explanation
    Provider.Microsoft -> R.string.account_edit_graph_reauthorization_explanation
    Provider.Zoho -> R.string.account_edit_zoho_reauthorization_explanation
    null -> R.string.account_edit_api_reauthorization_unavailable
}

@Composable
private fun Provider?.successResource(): Int = when (this) {
    Provider.Google -> R.string.account_edit_gmail_reauthorization_success
    Provider.Microsoft -> R.string.account_edit_graph_reauthorization_success
    Provider.Zoho -> R.string.account_edit_zoho_reauthorization_success
    null -> R.string.account_edit_api_reauthorization_done
}

@Composable
private fun Error.messageResource(provider: Provider?): Int = when (this) {
    Error.NotAvailable -> R.string.account_edit_api_reauthorization_unavailable

    Error.Canceled -> R.string.account_edit_api_reauthorization_canceled

    Error.IdentityMismatch -> when (provider) {
        Provider.Microsoft -> R.string.account_edit_graph_reauthorization_identity_mismatch
        else -> R.string.account_edit_gmail_reauthorization_identity_mismatch
    }

    Error.Failed -> R.string.account_edit_api_reauthorization_failure
}
