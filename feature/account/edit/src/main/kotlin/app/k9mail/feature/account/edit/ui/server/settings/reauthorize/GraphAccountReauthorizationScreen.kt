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
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.GraphAccountReauthorizationContract.Error
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.GraphAccountReauthorizationContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.contract.mvi.observe

@Composable
fun GraphAccountReauthorizationScreen(
    onBack: () -> Unit,
    viewModel: GraphAccountReauthorizationContract.ViewModel,
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
                title = stringResource(R.string.account_edit_graph_reauthorization_title),
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
                TextBodyMedium(text = stringResource(R.string.account_edit_graph_reauthorization_success))
                ButtonText(
                    text = stringResource(R.string.account_edit_graph_reauthorization_done),
                    onClick = onBack,
                )
            } else {
                TextBodyMedium(text = stringResource(R.string.account_edit_graph_reauthorization_explanation))
                state.value.error?.let { error ->
                    TextBodyMedium(text = error.toMessage())
                }

                if (state.value.isInitializing || state.value.isSaving) {
                    LoadingView(
                        message = if (state.value.isSaving) {
                            stringResource(R.string.account_edit_save_server_settings_loading_message)
                        } else {
                            stringResource(R.string.account_edit_graph_reauthorization_title)
                        },
                    )
                } else if (state.value.error != Error.NotAvailable) {
                    AccountOAuthView(
                        onOAuthResult = { dispatch(Event.OnOAuthResult(it)) },
                        viewModel = viewModel.accountOAuthViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun Error.toMessage(): String = when (this) {
    Error.NotAvailable -> stringResource(R.string.account_edit_graph_reauthorization_unavailable)
    Error.Canceled -> stringResource(R.string.account_edit_graph_reauthorization_canceled)
    Error.MicrosoftAccountMismatch -> stringResource(R.string.account_edit_graph_reauthorization_identity_mismatch)
    Error.Failed -> stringResource(R.string.account_edit_graph_reauthorization_failure)
}
