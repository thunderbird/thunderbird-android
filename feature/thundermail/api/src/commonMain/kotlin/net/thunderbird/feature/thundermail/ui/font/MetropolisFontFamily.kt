package net.thunderbird.feature.thundermail.ui.font

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.thunderbird.core.ui.compose.theme2.LocalThemeTypography
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.resources.Res
import net.thunderbird.feature.thundermail.resources.metropolis_regular
import net.thunderbird.feature.thundermail.resources.metropolis_semibold
import org.jetbrains.compose.resources.Font

internal val MetropolisFontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.metropolis_regular, FontWeight.Normal),
        Font(Res.font.metropolis_semibold, FontWeight.SemiBold),
    )

@Composable
fun UsingMetropolisTypography(content: @Composable () -> Unit) {
    val defaultTypography = MainTheme.typography
    val metropolisTypography = defaultTypography.copy(
        displayMedium = defaultTypography.displayMedium.copy(
            fontFamily = MetropolisFontFamily,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    CompositionLocalProvider(LocalThemeTypography provides metropolisTypography, content = content)
}
