package app.k9mail.ui.catalog

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
) {
    val themeState = remember { mutableStateOf(CatalogTheme.K9) }
    val themeVariantState = remember { mutableStateOf(CatalogThemeVariant.LIGHT) }

    CatalogThemeSwitch(
        theme = themeState.value,
        themeVariant = themeVariantState.value,
    ) {
        val contentPadding = WindowInsets.systemBars.asPaddingValues()

        CatalogContent(
            catalogTheme = themeState.value,
            catalogThemeVariant = themeVariantState.value,
            onThemeChange = {
                themeState.value = when (themeState.value) {
                    CatalogTheme.K9 -> CatalogTheme.THUNDERBIRD
                    CatalogTheme.THUNDERBIRD -> CatalogTheme.K9
                }
            },
            onThemeVariantChange = {
                themeVariantState.value = when (themeVariantState.value) {
                    CatalogThemeVariant.LIGHT -> CatalogThemeVariant.DARK
                    CatalogThemeVariant.DARK -> CatalogThemeVariant.LIGHT
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
