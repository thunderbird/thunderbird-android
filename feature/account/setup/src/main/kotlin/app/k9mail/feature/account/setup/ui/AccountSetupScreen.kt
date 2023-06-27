package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryScreen
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigScreen
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountSetupScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
    autoDiscoveryViewModel: AccountAutoDiscoveryContract.ViewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
    incomingViewModel: AccountIncomingConfigContract.ViewModel = koinViewModel<AccountIncomingConfigViewModel>(),
    outgoingViewModel: AccountOutgoingConfigContract.ViewModel = koinViewModel<AccountOutgoingConfigViewModel>(),
    optionsViewModel: AccountOptionsContract.ViewModel = koinViewModel<AccountOptionsViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.UpdateIncomingConfig -> incomingViewModel.initState(effect.state)
            is Effect.UpdateOutgoingConfig -> outgoingViewModel.initState(effect.state)
            is Effect.UpdateOptions -> optionsViewModel.initState(effect.state)
            is Effect.CollectExternalStates -> viewModel.event(
                Event.OnStateCollected(
                    autoDiscoveryState = autoDiscoveryViewModel.state.value,
                    incomingState = incomingViewModel.state.value,
                    outgoingState = outgoingViewModel.state.value,
                    optionsState = optionsViewModel.state.value,
                ),
            )

            is Effect.NavigateNext -> onFinish(effect.accountUuid)
            Effect.NavigateBack -> onBack()
        }
    }

    when (state.value.setupStep) {
        SetupStep.AUTO_CONFIG -> {
            AccountAutoDiscoveryScreen(
                onNext = { dispatch(Event.OnAutoDiscoveryFinished(it)) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = autoDiscoveryViewModel,
            )
        }

        SetupStep.INCOMING_CONFIG -> {
            AccountIncomingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = incomingViewModel,
            )
        }

        SetupStep.OUTGOING_CONFIG -> {
            AccountOutgoingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = outgoingViewModel,
            )
        }

        SetupStep.OPTIONS -> {
            AccountOptionsScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = optionsViewModel,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun AccountSetupScreenPreview() {
    AccountSetupScreen(
        onFinish = {},
        onBack = {},
    )
}
