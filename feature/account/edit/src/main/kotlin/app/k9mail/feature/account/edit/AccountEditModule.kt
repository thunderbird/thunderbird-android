package app.k9mail.feature.account.edit

import app.k9mail.feature.account.common.featureAccountCommonModule
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.usecase.LoadAccountState
import app.k9mail.feature.account.edit.ui.EditIncomingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.EditOutgoingServerSettingsViewModel
import app.k9mail.feature.account.oauth.featureAccountOAuthModule
import app.k9mail.feature.account.server.certificate.featureAccountServerCertificateModule
import app.k9mail.feature.account.server.settings.featureAccountServerSettingsModule
import app.k9mail.feature.account.server.validation.featureAccountServerValidationModule
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureAccountEditModule = module {
    includes(
        featureAccountCommonModule,
        featureAccountOAuthModule,
        featureAccountServerCertificateModule,
        featureAccountServerSettingsModule,
        featureAccountServerValidationModule,
    )

    factory<AccountEditDomainContract.UseCase.LoadAccountState> {
        LoadAccountState(
            accountStateLoader = get(),
            accountStateRepository = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        EditIncomingServerSettingsViewModel(
            accountUuid = accountUuid,
            accountStateLoader = get(),
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel { (accountUuid: String) ->
        EditOutgoingServerSettingsViewModel(
            accountUuid = accountUuid,
            accountStateLoader = get(),
            validator = get(),
            accountStateRepository = get(),
        )
    }
}
