package app.k9mail.feature.account.setup.ui.options.display

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AccountTopAppBar
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.preview.PreviewAccountStateRepository
import app.k9mail.feature.account.setup.R.string
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.ViewModel

@Composable
internal fun DisplayOptionsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.NavigateNext -> onNext()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadAccountState)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AccountTopAppBar(
                title = stringResource(id = string.account_setup_options_section_display_options),
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        DisplayOptionsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DisplayOptionsScreenK9Preview() {
    K9Theme {
        DisplayOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = DisplayOptionsViewModel(
                validator = DisplayOptionsValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
                accountOwnerNameProvider = { null },
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DisplayOptionsScreenThunderbirdPreview() {
    ThunderbirdTheme {
        DisplayOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = DisplayOptionsViewModel(
                validator = DisplayOptionsValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
                accountOwnerNameProvider = { null },
            ),
        )
    }
}
