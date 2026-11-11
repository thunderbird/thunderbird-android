package net.thunderbird.feature.debug.settings

import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionViewModel
import net.thunderbird.feature.debug.settings.navigation.DefaultSecretDebugSettingsNavigation
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsNavigation
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureDebugSettingsDiModule = module {
    single<SecretDebugSettingsNavigation> { DefaultSecretDebugSettingsNavigation() }
    viewModel {
        DebugNotificationSectionViewModel(
            stringsResourceManager = get(),
            accountProfileRepository = get(),
            notificationSender = get(),
            inAppNotificationStream = get(),
            notificationIconResourceProvider = get(),
        )
    }
    viewModel {
        DebugFeatureFlagSectionViewModel(
            bundleDefaults = get(),
            runtimeProvider = get(),
        )
    }
}
