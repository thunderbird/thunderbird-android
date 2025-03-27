package net.thunderbird.feature.account.settings

import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.DefaultAccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralResourceProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val featureAccountSettingsModule = module {
    single<AccountSettingsNavigation> { DefaultAccountSettingsNavigation() }

    factory<ResourceProvider.GeneralResourceProvider> {
        GeneralResourceProvider(
            context = androidContext(),
        )
    }
}
