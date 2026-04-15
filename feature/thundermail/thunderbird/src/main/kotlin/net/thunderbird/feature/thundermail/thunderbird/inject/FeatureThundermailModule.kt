package net.thunderbird.feature.thundermail.thunderbird.inject

import androidx.compose.ui.Modifier
import net.thunderbird.feature.thundermail.thunderbird.ui.thunderbirdBrandBackgroundProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.koin.dsl.module

val featureThundermailModule = module {
    single<BrandBackgroundModifierProvider> { Modifier.thunderbirdBrandBackgroundProvider() }
}
