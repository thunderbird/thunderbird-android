package net.thunderbird.feature.debug.settings

import net.thunderbird.feature.debug.settings.navigation.NoOpSecretDebugSettingsNavigation
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsNavigation
import org.koin.dsl.module

val featureDebugSettingsDiModule = module {
    single<SecretDebugSettingsNavigation> { NoOpSecretDebugSettingsNavigation }
}
