package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigScreen
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigViewModel
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigViewModel
import app.k9mail.feature.account.server.validation.KOIN_NAME_INCOMING_SERVER_VALIDATION
import app.k9mail.feature.account.server.validation.KOIN_NAME_OUTGOING_SERVER_VALIDATION
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationScreen
import app.k9mail.feature.account.server.validation.ui.ServerValidationViewModel
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryScreen
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named

@Suppress("LongMethod")
@Composable
fun AccountSetupScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
    autoDiscoveryViewModel: AccountAutoDiscoveryContract.ViewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
    incomingViewModel: AccountIncomingConfigContract.ViewModel = koinViewModel<AccountIncomingConfigViewModel>(),
    incomingValidationViewModel: ServerValidationContract.ViewModel = koinViewModel<ServerValidationViewModel>(
        named(KOIN_NAME_INCOMING_SERVER_VALIDATION),
    ),
    outgoingViewModel: AccountOutgoingConfigContract.ViewModel = koinViewModel<AccountOutgoingConfigViewModel>(),
    outgoingValidationViewModel: ServerValidationContract.ViewModel = koinViewModel<ServerValidationViewModel>(
        named(KOIN_NAME_OUTGOING_SERVER_VALIDATION),
    ),
    optionsViewModel: AccountOptionsContract.ViewModel = koinViewModel<AccountOptionsViewModel>(),
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
                onNext = { isAutomaticConfig ->
                    dispatch(
                        Event.OnAutoDiscoveryFinished(
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
            ServerValidationScreen(
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
            ServerValidationScreen(
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
