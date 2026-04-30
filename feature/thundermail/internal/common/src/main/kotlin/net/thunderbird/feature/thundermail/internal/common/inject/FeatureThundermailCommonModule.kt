package net.thunderbird.feature.thundermail.internal.common.inject

import net.thunderbird.feature.thundermail.internal.common.navigation.DefaultThundermailNavigation
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import org.koin.dsl.module

val featureThundermailCommonModule = module {
    single<ThundermailNavigation> { DefaultThundermailNavigation() }
}
