package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import app.k9mail.core.ui.compose.theme.color.Colors
import app.k9mail.core.ui.compose.theme.color.LocalColors
import app.k9mail.core.ui.compose.theme.color.SetSystemBarsColor
import app.k9mail.core.ui.compose.theme.color.toMaterialColors

@Composable
fun MainTheme(
    lightColorPalette: Colors,
    darkColorPalette: Colors,
    lightImages: Images,
    darkImages: Images,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        darkColorPalette
    } else {
        lightColorPalette
    }
    val images = if (darkTheme) {
        darkImages
    } else {
        lightImages
    }

    SetSystemBarsColor(
        darkTheme = darkTheme,
        color = colors.toolbar,
    )

    val shapes = MainTheme.shapes

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalElevations provides Elevations(),
        LocalImages provides images,
        LocalSizes provides Sizes(),
        LocalSpacings provides Spacings(),
        LocalShapes provides shapes,
    ) {
        MaterialTheme(
            colors = colors.toMaterialColors(),
            typography = typography,
            shapes = shapes.toMaterialShapes(),
            content = content,
        )
    }
}

object MainTheme {
    val colors: Colors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current

    val spacings: Spacings
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacings.current

    val sizes: Sizes
        @Composable
        @ReadOnlyComposable
        get() = LocalSizes.current

    val elevations: Elevations
        @Composable
        @ReadOnlyComposable
        get() = LocalElevations.current

    val images: Images
        @Composable
        @ReadOnlyComposable
        get() = LocalImages.current
}
