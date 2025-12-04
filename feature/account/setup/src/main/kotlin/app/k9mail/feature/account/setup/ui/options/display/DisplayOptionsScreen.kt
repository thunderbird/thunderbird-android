package app.k9mail.feature.account.setup.ui.options.display

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
internal fun DisplayOptionsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
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
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                modifier = Modifier.imePadding(),
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        DisplayOptionsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
            brandName = brandNameProvider.brandName,
        )
    }
}
