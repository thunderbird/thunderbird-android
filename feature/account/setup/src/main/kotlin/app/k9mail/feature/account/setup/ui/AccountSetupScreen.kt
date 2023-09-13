package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsScreen
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsScreen
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsViewModel
import app.k9mail.feature.account.server.validation.ui.IncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.OutgoingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationScreen
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

@Suppress("LongMethod")
@Composable
fun AccountSetupScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
    autoDiscoveryViewModel: AccountAutoDiscoveryContract.ViewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
    incomingViewModel: IncomingServerSettingsContract.ViewModel = koinViewModel<IncomingServerSettingsViewModel>(),
    incomingValidationViewModel: ServerValidationContract.ViewModel =
        koinViewModel<IncomingServerValidationViewModel>(),
    outgoingViewModel: OutgoingServerSettingsContract.ViewModel = koinViewModel<OutgoingServerSettingsViewModel>(),
    outgoingValidationViewModel: ServerValidationContract.ViewModel =
        koinViewModel<OutgoingServerValidationViewModel>(),
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
            IncomingServerSettingsScreen(
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
            OutgoingServerSettingsScreen(
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
