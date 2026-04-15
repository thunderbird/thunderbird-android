package net.thunderbird.feature.thundermail.thunderbird.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

/**
 * Provides a [BrandBackgroundModifierProvider] that applies a custom Thunderbird-themed
 * brand background to a [Modifier].
 *
 * @return A [BrandBackgroundModifierProvider] containing the modifier with the applied
 * brand background and decorative layers.
 */
@Suppress("MagicNumber")
internal fun Modifier.thunderbirdBrandBackgroundProvider(): BrandBackgroundModifierProvider =
    BrandBackgroundModifierProvider {
        this@thunderbirdBrandBackgroundProvider then if (isSystemInDarkTheme()) {
            Modifier.thunderbirdBrandDark()
        } else {
            Modifier.thunderbirdBrandLight()
        }
    }
