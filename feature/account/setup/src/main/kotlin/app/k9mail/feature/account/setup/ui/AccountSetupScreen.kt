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
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryScreen
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigScreen
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigViewModel
import app.k9mail.feature.account.setup.ui.validation.AccountValidationScreen
import app.k9mail.feature.account.setup.ui.validation.AccountValidationViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named

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
                viewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
            )
        }

        SetupStep.INCOMING_CONFIG -> {
            AccountIncomingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = koinViewModel<AccountIncomingConfigViewModel>(),
            )
        }

        SetupStep.INCOMING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = koinViewModel<AccountValidationViewModel>(named(NAME_INCOMING_VALIDATION)),
            )
        }

        SetupStep.OUTGOING_CONFIG -> {
            AccountOutgoingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = koinViewModel<AccountOutgoingConfigViewModel>(),
            )
        }

        SetupStep.OUTGOING_VALIDATION -> {
            AccountValidationScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
                viewModel = koinViewModel<AccountValidationViewModel>(named(NAME_OUTGOING_VALIDATION)),
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
