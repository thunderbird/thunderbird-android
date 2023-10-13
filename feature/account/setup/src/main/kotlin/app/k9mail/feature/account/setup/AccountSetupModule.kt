package app.k9mail.feature.account.setup

import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.service.RealAutoDiscoveryService
import app.k9mail.feature.account.common.featureAccountCommonModule
import app.k9mail.feature.account.oauth.featureAccountOAuthModule
import app.k9mail.feature.account.server.settings.featureAccountServerSettingsModule
import app.k9mail.feature.account.server.validation.featureAccountServerValidationModule
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.CreateAccount
import app.k9mail.feature.account.setup.domain.usecase.GetAutoDiscovery
import app.k9mail.feature.account.setup.domain.usecase.GetSpecialFolderOptions
import app.k9mail.feature.account.setup.domain.usecase.ValidateSpecialFolderOptions
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryValidator
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsValidator
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersFormUiModel
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersViewModel
import com.fsck.k9.mail.folders.FolderFetcher
import com.fsck.k9.mail.store.imap.ImapFolderFetcher
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureAccountSetupModule: Module = module {
    includes(
        featureAccountCommonModule,
        featureAccountOAuthModule,
        featureAccountServerValidationModule,
        featureAccountServerSettingsModule,
    )

    single<OkHttpClient> {
        OkHttpClient()
    }

    single<AutoDiscoveryService> {
        RealAutoDiscoveryService(
            okHttpClient = get(),
        )
    }

    single<DomainContract.UseCase.GetAutoDiscovery> {
        GetAutoDiscovery(
            service = get(),
            oauthProvider = get(),
        )
    }

    factory<DomainContract.UseCase.CreateAccount> {
        CreateAccount(
            accountCreator = get(),
        )
    }

    factory<AccountAutoDiscoveryContract.Validator> { AccountAutoDiscoveryValidator() }
    factory<AccountOptionsContract.Validator> { AccountOptionsValidator() }

    viewModel {
        AccountAutoDiscoveryViewModel(
            validator = get(),
            getAutoDiscovery = get(),
            accountStateRepository = get(),
            oAuthViewModel = get(),
        )
    }

    factory<FolderFetcher> {
        ImapFolderFetcher(
            trustedSocketFactory = get(),
            oAuth2TokenProviderFactory = get(),
            clientIdAppName = get(named("ClientIdAppName")),
            clientIdAppVersion = get(named("ClientIdAppVersion")),
        )
    }

    factory<DomainContract.UseCase.GetSpecialFolderOptions> {
        GetSpecialFolderOptions(
            folderFetcher = get(),
            accountStateRepository = get(),
            authStateStorage = get(),
        )
    }

    factory<DomainContract.UseCase.ValidateSpecialFolderOptions> {
        ValidateSpecialFolderOptions()
    }

    factory<SpecialFoldersContract.FormUiModel> {
        SpecialFoldersFormUiModel()
    }

    viewModel {
        SpecialFoldersViewModel(
            formUiModel = get(),
            getSpecialFolderOptions = get(),
            validateSpecialFolderOptions = get(),
            accountStateRepository = get(),
        )
    }

    viewModel {
        AccountOptionsViewModel(
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel {
        CreateAccountViewModel(
            createAccount = get(),
            accountStateRepository = get(),
        )
    }
}
