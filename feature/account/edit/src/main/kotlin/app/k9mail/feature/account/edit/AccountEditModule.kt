package app.k9mail.feature.account.edit

import app.k9mail.feature.account.common.featureAccountCommonModule
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.usecase.GetAccountState
import app.k9mail.feature.account.edit.domain.usecase.LoadAccountState
import app.k9mail.feature.account.edit.domain.usecase.SaveServerSettings
import app.k9mail.feature.account.edit.navigation.AccountEditNavigation
import app.k9mail.feature.account.edit.navigation.DefaultAccountEditNavigation
import app.k9mail.feature.account.edit.ui.server.settings.modify.ModifyIncomingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.modify.ModifyOutgoingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveIncomingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveOutgoingServerSettingsViewModel
import app.k9mail.feature.account.oauth.featureAccountOAuthModule
import app.k9mail.feature.account.server.certificate.featureAccountServerCertificateModule
import app.k9mail.feature.account.server.settings.featureAccountServerSettingsModule
import app.k9mail.feature.account.server.validation.featureAccountServerValidationModule
import net.thunderbird.feature.account.AccountIdFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAccountEditModule = module {
    includes(
        featureAccountCommonModule,
        featureAccountOAuthModule,
        featureAccountServerCertificateModule,
        featureAccountServerSettingsModule,
        featureAccountServerValidationModule,
    )

    single<AccountEditNavigation> { DefaultAccountEditNavigation() }

    factory<AccountEditDomainContract.UseCase.LoadAccountState> {
        LoadAccountState(
            accountStateLoader = get(),
            accountStateRepository = get(),
        )
    }

    factory<AccountEditDomainContract.UseCase.GetAccountState> {
        GetAccountState(
            accountStateRepository = get(),
        )
    }

    factory<AccountEditDomainContract.UseCase.SaveServerSettings> {
        SaveServerSettings(
            getAccountState = get(),
            serverSettingsUpdater = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        ModifyIncomingServerSettingsViewModel(
            accountId = AccountIdFactory.of(accountUuid),
            accountStateLoader = get(),
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        ModifyOutgoingServerSettingsViewModel(
            accountId = AccountIdFactory.of(accountUuid),
            accountStateLoader = get(),
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        SaveIncomingServerSettingsViewModel(
            accountId = AccountIdFactory.of(accountUuid),
            saveServerSettings = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        SaveOutgoingServerSettingsViewModel(
            accountId = AccountIdFactory.of(accountUuid),
            saveServerSettings = get(),
        )
    }
}
