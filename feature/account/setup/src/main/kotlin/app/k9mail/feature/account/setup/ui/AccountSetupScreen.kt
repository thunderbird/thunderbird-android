package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigScreen
import app.k9mail.feature.account.setup.ui.manualconfig.AccountManualConfigScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountSetupScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel = koinViewModel<AccountSetupViewModel>(),
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
                onNextClick = { dispatch(Event.OnNext) },
                onBackClick = { dispatch(Event.OnBack) },
            )
        }

        SetupStep.MANUAL_CONFIG -> {
            AccountManualConfigScreen(
                onNextClick = { dispatch(Event.OnNext) },
                onBackClick = { dispatch(Event.OnBack) },
            )
        }

        SetupStep.OPTIONS -> {
            AccountOptionsScreen(
                onFinishClick = { dispatch(Event.OnNext) },
                onBackClick = { dispatch(Event.OnBack) },
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
