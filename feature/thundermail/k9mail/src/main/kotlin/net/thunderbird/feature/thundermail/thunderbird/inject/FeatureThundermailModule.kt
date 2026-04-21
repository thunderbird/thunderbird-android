package net.thunderbird.feature.thundermail.thunderbird.inject

import net.thunderbird.feature.thundermail.thunderbird.ui.K9MailBrandBackgroundProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.koin.dsl.module

val featureThundermailModule = module {
    single<BrandBackgroundModifierProvider> { K9MailBrandBackgroundProvider }
}
