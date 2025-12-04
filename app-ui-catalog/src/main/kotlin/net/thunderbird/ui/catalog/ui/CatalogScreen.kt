package net.thunderbird.ui.catalog.ui

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
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
    val activity = LocalActivity.current as Activity
    val (state, dispatch) = viewModel.observe(handleEffect = {})
    val navController = rememberNavController()

    LaunchedEffect(state.value.themeVariant) {
        val isLight = state.value.themeVariant == CatalogContract.ThemeVariant.LIGHT
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = isLight
        }
    }

    ThemeSwitch(
        theme = state.value.theme,
        themeVariant = state.value.themeVariant,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
        ) {
            CatalogContent(
                navController = navController,
                state = state.value,
                onThemeChanged = { dispatch(OnThemeChanged) },
                onThemeVariantChanged = { dispatch(OnThemeVariantChanged) },
            )
        }
    }
}
