package app.k9mail.feature.account.setup.ui.options

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R.string
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopAppBar
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.ViewModel

@Composable
internal fun AccountOptionsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.NavigateNext -> {
                Toast.makeText(context, "Input is valid", Toast.LENGTH_SHORT).show() // TODO remove
                onNext()
            }
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AccountSetupTopAppBar(
                title = stringResource(id = string.account_setup_options_top_bar_title),
            )
        },
        bottomBar = {
            AccountSetupBottomBar(
                nextButtonText = stringResource(id = string.account_setup_button_finish),
                backButtonText = stringResource(id = string.account_setup_button_back),
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountOptionsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsScreenK9Preview() {
    K9Theme {
        AccountOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountOptionsViewModel(
                validator = AccountOptionsValidator(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountOptionsViewModel(
                validator = AccountOptionsValidator(),
            ),
        )
    }
}
