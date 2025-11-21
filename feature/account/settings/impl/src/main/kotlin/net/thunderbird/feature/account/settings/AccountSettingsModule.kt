package net.thunderbird.feature.account.settings

import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.DefaultAccountSettingsNavigation
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.usecase.GetAccountName
import net.thunderbird.feature.account.settings.impl.domain.usecase.GetAccountProfile
import net.thunderbird.feature.account.settings.impl.domain.usecase.UpdateAvatarImage
import net.thunderbird.feature.account.settings.impl.domain.usecase.UpdateGeneralSettings
import net.thunderbird.feature.account.settings.impl.domain.usecase.ValidateAccountName
import net.thunderbird.feature.account.settings.impl.domain.usecase.ValidateAvatarMonogram
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsValidator
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureAccountSettingsModule = module {
    single<AccountSettingsNavigation> { DefaultAccountSettingsNavigation() }

    factory<UseCase.GetAccountName> {
        GetAccountName(
            repository = get(),
        )
    }

    factory<UseCase.GetAccountProfile> {
        GetAccountProfile(
            repository = get(),
        )
    }

    factory<UseCase.UpdateAvatarImage> {
        UpdateAvatarImage(
            repository = get(),
        )
    }

    factory<UseCase.UpdateGeneralSettings> {
        UpdateGeneralSettings(
            repository = get(),
        )
    }

    factory<GeneralSettingsContract.Validator> {
        GeneralSettingsValidator(
            accountNameValidator = ValidateAccountName(),
            avatarMonogramValidator = ValidateAvatarMonogram(),
        )
    }

    factory<GeneralSettingsContract.SettingsBuilder> {
        GeneralSettingsBuilder(
            resources = get<StringsResourceManager>(),
            accountColors = get<ImmutableList<Int>>(named("AccountColors")),
            monogramCreator = get(),
            validator = get(),
            featureFlagProvider = get(),
            iconCatalog = get(),
        )
    }

    viewModel { params ->
        GeneralSettingsViewModel(
            accountId = params.get(),
            getAccountName = get(),
            getAccountProfile = get(),
            updateGeneralSettings = get(),
        )
    }
}
