package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigScreen
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigScreen
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountSetupScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
    outgoingViewModel: AccountOutgoingConfigContract.ViewModel = koinViewModel<AccountOutgoingConfigViewModel>(),
    optionsViewModel: AccountOptionsContract.ViewModel = koinViewModel<AccountOptionsViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.NavigateNext -> onFinish()
        }
    }

    when (state.value.setupStep) {
        SetupStep.AUTO_CONFIG -> {
            AccountAutoConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
            )
        }

        SetupStep.INCOMING_CONFIG -> {
            AccountIncomingConfigScreen(
                onNext = { dispatch(Event.OnNext) },
                onBack = { dispatch(Event.OnBack) },
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
