package net.thunderbird.components.ui.bolt.theme

import androidx.compose.material3.darkColorScheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.components.ui.bolt.theme.thunderbird.lightThemeColorScheme

class ThemeColorSchemeTest {

    @Test
    fun `toBoltColorScheme should map Material color roles`() {
        val fallback = lightThemeColorScheme
        val materialColorScheme = darkColorScheme()
        val expected = fallback.copy(
            primary = materialColorScheme.primary,
            onPrimary = materialColorScheme.onPrimary,
            primaryContainer = materialColorScheme.primaryContainer,
            onPrimaryContainer = materialColorScheme.onPrimaryContainer,
            secondary = materialColorScheme.secondary,
            onSecondary = materialColorScheme.onSecondary,
            secondaryContainer = materialColorScheme.secondaryContainer,
            onSecondaryContainer = materialColorScheme.onSecondaryContainer,
            tertiary = materialColorScheme.tertiary,
            onTertiary = materialColorScheme.onTertiary,
            tertiaryContainer = materialColorScheme.tertiaryContainer,
            onTertiaryContainer = materialColorScheme.onTertiaryContainer,
            error = materialColorScheme.error,
            onError = materialColorScheme.onError,
            errorContainer = materialColorScheme.errorContainer,
            onErrorContainer = materialColorScheme.onErrorContainer,
            surfaceDim = materialColorScheme.surfaceDim,
            surface = materialColorScheme.surface,
            surfaceBright = materialColorScheme.surfaceBright,
            onSurface = materialColorScheme.onSurface,
            onSurfaceVariant = materialColorScheme.onSurfaceVariant,
            surfaceContainerLowest = materialColorScheme.surfaceContainerLowest,
            surfaceContainerLow = materialColorScheme.surfaceContainerLow,
            surfaceContainer = materialColorScheme.surfaceContainer,
            surfaceContainerHigh = materialColorScheme.surfaceContainerHigh,
            surfaceContainerHighest = materialColorScheme.surfaceContainerHighest,
            inverseSurface = materialColorScheme.inverseSurface,
            inverseOnSurface = materialColorScheme.inverseOnSurface,
            inversePrimary = materialColorScheme.inversePrimary,
            outline = materialColorScheme.outline,
            outlineVariant = materialColorScheme.outlineVariant,
            scrim = materialColorScheme.scrim,
        )

        val result = materialColorScheme.toBoltColorScheme(fallback)

        assertThat(result).isEqualTo(expected)
    }
}
