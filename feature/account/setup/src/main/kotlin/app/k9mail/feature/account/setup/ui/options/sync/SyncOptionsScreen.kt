package app.k9mail.feature.account.setup.ui.options.sync

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold

@Composable
internal fun SharedTransitionScope.SyncOptionsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
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

    val lazyListState = rememberLazyListState()
    ThundermailScaffold(
        header = {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
                sharedTransitionScope = this,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        subHeaderText = stringResource(R.string.account_setup_options_section_sync_options),
        bottomBar = { paddingValues, containerColor ->
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                modifier = Modifier
                    .imePadding()
                    .background(containerColor)
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.default)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        maxWidth = Dp.Unspecified,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        SyncOptionsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = responsivePaddingValues,
            maxWidth = maxWidth,
            listState = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .consumeWindowInsets(scaffoldPaddingValues)
                .imePadding(),
        )
    }
}
