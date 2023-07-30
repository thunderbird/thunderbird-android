package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryScreen
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.setup.ui.validation.AccountValidationScreen
import org.koin.androidx.compose.koinViewModel

@Suppress("LongMethod")
@Composable
fun AccountSetupScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onFinish(effect.accountUuid)
            Effect.NavigateBack -> onBack()
        }
    }

    when (state.value.setupStep) {
        SetupStep.AUTO_CONFIG -> {
            AccountAutoDiscoveryScreen(
                onNext = { autoDiscoveryState, isAutomaticConfig ->
                    dispatch(
                        Event.OnAutoDiscoveryFinished(
                            autoDiscoveryState,
                            isAutomaticConfig,
                        ),
                    )
                },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.autoDiscoveryViewModel,
            )
        }

        SetupStep.INCOMING_CONFIG -> {
            AccountIncomingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.incomingViewModel,
            )
        }

        SetupStep.INCOMING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.incomingValidationViewModel,
            )
        }

        SetupStep.OUTGOING_CONFIG -> {
            AccountOutgoingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.outgoingViewModel,
            )
        }

        SetupStep.OUTGOING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.outgoingValidationViewModel,
            )
        }

        SetupStep.OPTIONS -> {
            AccountOptionsScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = viewModel.optionsViewModel,
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
