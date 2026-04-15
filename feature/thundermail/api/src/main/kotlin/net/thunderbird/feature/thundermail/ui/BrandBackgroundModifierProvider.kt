package net.thunderbird.feature.thundermail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

/**
 * A provider interface used to apply a brand-specific background to a [Modifier].
 */
fun interface BrandBackgroundModifierProvider {
    @Composable
    fun Modifier.brandBackground(): Modifier
}

/**
 * Applies a brand-specific background to the [Modifier].
 *
 * Injects a [BrandBackgroundModifierProvider] via Koin to retrieve and
 * apply the branded styling defined in the current theme or configuration.
 *
 * @return A [Modifier] with the brand background applied.
 */
@Composable
fun Modifier.brandBackground(): Modifier {
    val provider = koinInject<BrandBackgroundModifierProvider>()
    return this then with(provider) { Modifier.brandBackground() }
}
