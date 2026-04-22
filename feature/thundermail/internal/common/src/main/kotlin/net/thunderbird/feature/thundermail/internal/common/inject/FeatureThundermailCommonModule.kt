package net.thunderbird.feature.thundermail.internal.common.inject

import net.thunderbird.feature.thundermail.internal.common.ui.screen.DefaultAddThundermailAccountScreenProvider
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenProvider
import org.koin.dsl.module

val featureThundermailCommonModule = module {
    single<AddThundermailAccountScreenProvider> { DefaultAddThundermailAccountScreenProvider }
}
