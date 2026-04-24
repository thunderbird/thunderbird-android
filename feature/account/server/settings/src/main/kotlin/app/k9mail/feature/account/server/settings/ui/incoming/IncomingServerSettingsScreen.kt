package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.organism.ThundermailToolbar
import org.koin.compose.koinInject

@Composable
fun IncomingServerSettingsScreen(
    onNext: (IncomingServerSettingsContract.State) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
    brandNameProvider: BrandNameProvider = koinInject(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext(viewModel.state.value)
            is Effect.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadAccountState)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            if (viewModel.mode == InteractionMode.Edit) {
                TopAppBarWithBackButton(
                    title = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
                    onBackClick = { dispatch(Event.OnBackClicked) },
                )
            } else {
                ResponsiveWidthContainer { paddingValues ->
                    ThundermailToolbar(
                        header = { AppTitleTopHeader(brandNameProvider.brandName) },
                        subHeaderText = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
                        maxWidth = Dp.Unspecified,
                        contentPadding = PaddingValues(
                            horizontal = MainTheme.spacings.quadruple,
                        ),
                        modifier = Modifier.padding(paddingValues),
                    )
                }
            }
        },
        bottomBar = {
            ResponsiveWidthContainer { paddingValues ->
                // Elevate the bottom bar when some scrollable content is "underneath" it
                val containerColor by animateColorAsState(
                    targetValue = if (lazyListState.canScrollForward) {
                        MainTheme.colors.surfaceContainerLowest.copy(alpha = .25f)
                    } else {
                        Color.Transparent
                    },
                    label = "WizardNavigationBar",
                )
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
            }
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        IncomingServerSettingsContent(
            mode = viewModel.mode,
            onEvent = { dispatch(it) },
            state = state.value,
            listState = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        )
    }
}
