package net.thunderbird.feature.thundermail.thunderbird.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

/**
 * Provides a [BrandBackgroundModifierProvider] that applies a custom K-9 Mail-themed
 * brand background to a [Modifier].
 *
 * @return A [BrandBackgroundModifierProvider] containing the modifier with the applied
 * brand background and decorative layers.
 */
internal object K9MailBrandBackgroundProvider : BrandBackgroundModifierProvider {
    @Composable
    override fun Modifier.brandBackground(): Modifier {
        return this then Modifier.background(MainTheme.colors.surface)
    }
}
