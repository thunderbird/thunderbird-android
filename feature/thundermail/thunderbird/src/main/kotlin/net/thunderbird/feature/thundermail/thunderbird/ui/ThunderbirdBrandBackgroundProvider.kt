package net.thunderbird.feature.thundermail.thunderbird.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

/**
 * Provides a [BrandBackgroundModifierProvider] that applies a custom Thunderbird-themed
 * brand background to a [Modifier].
 *
 * @return A [BrandBackgroundModifierProvider] containing the modifier with the applied
 * brand background and decorative layers.
 */
internal object ThunderbirdBrandBackgroundProvider : BrandBackgroundModifierProvider {
    @Composable
    override fun Modifier.brandBackground(): Modifier {
        return this then if (isSystemInDarkTheme()) {
            Modifier.thunderbirdBrandDark()
        } else {
            Modifier.thunderbirdBrandLight()
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    Box(modifier = with(ThunderbirdBrandBackgroundProvider) { Modifier.fillMaxSize().brandBackground() })
}
