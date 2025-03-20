package net.thunderbird.ui.catalog.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import net.thunderbird.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import net.thunderbird.ui.catalog.ui.CatalogContract.ViewModel
import net.thunderbird.ui.catalog.ui.common.theme.ThemeSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<CatalogViewModel>(),
) {
    val (state, dispatch) = viewModel.observe(handleEffect = {})

    ThemeSwitch(
        theme = state.value.theme,
        themeVariant = state.value.themeVariant,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .then(modifier),
        ) {
            CatalogContent(
                state = state.value,
                onThemeChanged = { dispatch(OnThemeChanged) },
                onThemeVariantChanged = { dispatch(OnThemeVariantChanged) },
            )
        }
    }
}
