package net.thunderbird.feature.debugSettings.inject

import net.thunderbird.feature.debugSettings.navigation.DefaultSecretDebugSettingsNavigation
import net.thunderbird.feature.debugSettings.navigation.SecretDebugSettingsNavigation
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureDebugSettingsModule = module {
    single<SecretDebugSettingsNavigation> { DefaultSecretDebugSettingsNavigation() }
    viewModel {
        DebugNotificationSectionViewModel(
            accountManager = get(),
            notificationSender = get(),
        )
    }
}
