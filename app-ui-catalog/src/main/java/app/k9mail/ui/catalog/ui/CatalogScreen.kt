package app.k9mail.ui.catalog.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.common.theme.ThemeSwitch

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
) {
    val themeState = remember { mutableStateOf(Theme.K9) }
    val themeVariantState = remember { mutableStateOf(ThemeVariant.LIGHT) }

    ThemeSwitch(
        theme = themeState.value,
        themeVariant = themeVariantState.value,
    ) {
        val contentPadding = WindowInsets.systemBars.asPaddingValues()

        CatalogContent(
            theme = themeState.value,
            themeVariant = themeVariantState.value,
            onThemeChange = {
                themeState.value = when (themeState.value) {
                    Theme.K9 -> Theme.THUNDERBIRD
                    Theme.THUNDERBIRD -> Theme.K9
                }
            },
            onThemeVariantChange = {
                themeVariantState.value = when (themeVariantState.value) {
                    ThemeVariant.LIGHT -> ThemeVariant.DARK
                    ThemeVariant.DARK -> ThemeVariant.LIGHT
                }
            },
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
        )
    }
}

@DevicePreviews
@Composable
internal fun CatalogScreenPreview() {
    CatalogScreen()
}
