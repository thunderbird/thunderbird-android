package net.thunderbird.feature.account.settings

import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.DefaultAccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.usecase.GetAccountName
import net.thunderbird.feature.account.settings.impl.domain.usecase.GetGeneralSettings
import net.thunderbird.feature.account.settings.impl.domain.usecase.UpdateGeneralSettings
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralResourceProvider
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAccountSettingsModule = module {
    single<AccountSettingsNavigation> { DefaultAccountSettingsNavigation() }

    factory<ResourceProvider.GeneralResourceProvider> {
        GeneralResourceProvider(
            context = androidContext(),
        )
    }

    factory<UseCase.GetAccountName> {
        GetAccountName(
            repository = get(),
        )
    }

    factory<UseCase.GetGeneralSettings> {
        GetGeneralSettings(
            repository = get(),
            resourceProvider = get(),
            monogramCreator = get(),
        )
    }

    factory<UseCase.UpdateGeneralSettings> {
        UpdateGeneralSettings(
            repository = get(),
        )
    }

    viewModel { params ->
        GeneralSettingsViewModel(
            accountId = params.get(),
            getAccountName = get(),
            getGeneralSettings = get(),
            updateGeneralSettings = get(),
        )
    }
}
