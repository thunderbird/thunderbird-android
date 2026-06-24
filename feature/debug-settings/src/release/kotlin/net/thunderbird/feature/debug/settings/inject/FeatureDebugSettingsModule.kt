package net.thunderbird.feature.debug.settings.inject

import net.thunderbird.feature.debug.settings.navigation.DefaultSecretDebugSettingsNavigation
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsNavigation
import org.koin.dsl.module

val featureDebugSettingsModule = module {
    single<SecretDebugSettingsNavigation> { DefaultSecretDebugSettingsNavigation() }
}
