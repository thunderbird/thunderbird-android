package net.thunderbird.feature.account.settings

import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.DefaultAccountSettingsNavigation
import org.koin.dsl.module

val featureAccountSettingsModule = module {
    single<AccountSettingsNavigation> { DefaultAccountSettingsNavigation() }
}
