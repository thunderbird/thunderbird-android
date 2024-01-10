package app.k9mail.feature.account.setup.ui.options.sync

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
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.ViewModel

@Composable
internal fun SyncOptionsScreen(
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
                title = stringResource(id = string.account_setup_options_section_sync_options),
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
        SyncOptionsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SyncOptionsScreenK9Preview() {
    K9Theme {
        SyncOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = SyncOptionsViewModel(
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SyncOptionsScreenThunderbirdPreview() {
    ThunderbirdTheme {
        SyncOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = SyncOptionsViewModel(
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}
