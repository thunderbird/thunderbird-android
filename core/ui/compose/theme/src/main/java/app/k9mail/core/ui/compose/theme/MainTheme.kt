package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MainTheme(
    lightColorPalette: Colors,
    darkColorPalette: Colors,
    lightImages: Images,
    darkImages: Images,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
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

    CompositionLocalProvider(
        LocalElevations provides Elevations(),
        LocalImages provides images,
        LocalSizes provides Sizes(),
        LocalSpacings provides Spacings()
    ) {
        MaterialTheme(
            colors = colors,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

object MainTheme {
    val colors: Colors
        @Composable
        get() = MaterialTheme.colors

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes

    val spacings: Spacings
        @Composable
        get() = LocalSpacings.current

    val sizes: Sizes
        @Composable
        get() = LocalSizes.current

    val elevations: Elevations
        @Composable
        get() = LocalElevations.current

    val images: Images
        @Composable
        get() = LocalImages.current
}
