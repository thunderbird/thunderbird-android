package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.NAME_INCOMING_VALIDATION
import app.k9mail.feature.account.setup.NAME_OUTGOING_VALIDATION
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
import app.k9mail.feature.account.setup.ui.incoming.toValidationState
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigViewModel
import app.k9mail.feature.account.setup.ui.outgoing.toValidationState
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import app.k9mail.feature.account.setup.ui.validation.AccountValidationScreen
import app.k9mail.feature.account.setup.ui.validation.AccountValidationViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun AccountSetupScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
    autoDiscoveryViewModel: AccountAutoDiscoveryContract.ViewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
    incomingViewModel: AccountIncomingConfigContract.ViewModel = koinViewModel<AccountIncomingConfigViewModel>(),
    incomingValidationViewModel: AccountValidationContract.ViewModel = koinViewModel<AccountValidationViewModel>(
        named(
            NAME_INCOMING_VALIDATION,
        ),
    ),
    outgoingViewModel: AccountOutgoingConfigContract.ViewModel = koinViewModel<AccountOutgoingConfigViewModel>(),
    outgoingValidationViewModel: AccountValidationContract.ViewModel = koinViewModel<AccountValidationViewModel>(
        named(
            NAME_OUTGOING_VALIDATION,
        ),
    ),
    optionsViewModel: AccountOptionsContract.ViewModel = koinViewModel<AccountOptionsViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.UpdateIncomingConfig -> incomingViewModel.initState(effect.state)
            is Effect.UpdateIncomingConfigValidation -> {
                incomingValidationViewModel.initState(incomingViewModel.state.value.toValidationState())
            }

            is Effect.UpdateOutgoingConfig -> outgoingViewModel.initState(effect.state)
            is Effect.UpdateOutgoingConfigValidation -> {
                outgoingValidationViewModel.initState(outgoingViewModel.state.value.toValidationState())
            }

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
                onNext = { autoDiscoveryState, isAutomaticConfig ->
                    dispatch(
                        Event.OnAutoDiscoveryFinished(
                            autoDiscoveryState,
                            isAutomaticConfig,
                        ),
                    )
                },
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

        SetupStep.INCOMING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = incomingValidationViewModel,
            )
        }

        SetupStep.OUTGOING_CONFIG -> {
            AccountOutgoingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = outgoingViewModel,
            )
        }

        SetupStep.OUTGOING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = outgoingValidationViewModel,
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
