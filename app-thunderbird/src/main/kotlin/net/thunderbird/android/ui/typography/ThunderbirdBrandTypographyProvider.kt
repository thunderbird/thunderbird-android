package net.thunderbird.android.ui.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.core.ui.compose.theme2.LocalThemeTypography
import net.thunderbird.core.ui.compose.theme2.MainTheme

internal class ThunderbirdBrandTypographyProvider : BrandTypographyProvider {
    @Composable
    override fun UsingTypography(content: @Composable (() -> Unit)) {
        val defaultTypography = MainTheme.typography
        val metropolisTypography = defaultTypography.copy(
            displayMedium = defaultTypography.displayMedium.copy(
                fontFamily = MetropolisFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        CompositionLocalProvider(LocalThemeTypography provides metropolisTypography, content = content)
    }
}
