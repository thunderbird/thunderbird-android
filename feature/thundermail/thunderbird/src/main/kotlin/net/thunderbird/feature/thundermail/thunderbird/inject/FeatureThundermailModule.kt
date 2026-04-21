package net.thunderbird.feature.thundermail.thunderbird.inject

import net.thunderbird.feature.thundermail.thunderbird.ui.ThunderbirdBrandBackgroundProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.koin.dsl.module

val featureThundermailModule = module {
    single<BrandBackgroundModifierProvider> { ThunderbirdBrandBackgroundProvider }
}
