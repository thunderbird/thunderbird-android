package app.k9mail.feature.account.setup

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryRegistry
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.service.RealAutoDiscoveryRegistry
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
import app.k9mail.feature.account.setup.navigation.AccountSetupNavigation
import app.k9mail.feature.account.setup.navigation.DefaultAccountSetupNavigation
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryValidator
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountViewModel
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsValidator
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsViewModel
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsViewModel
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

    single<AccountSetupNavigation> { DefaultAccountSetupNavigation() }

    single<OkHttpClient> {
        OkHttpClient()
    }

    single<AutoDiscoveryRegistry> {
        val extraAutoDiscoveries = get<List<AutoDiscovery>>(named("extraAutoDiscoveries"))
        RealAutoDiscoveryRegistry(
            autoDiscoveries = RealAutoDiscoveryRegistry.createDefaultAutoDiscoveries(
                okHttpClient = get(),
            ) + extraAutoDiscoveries,
        )
    }

    single<AutoDiscoveryService> {
        RealAutoDiscoveryService(
            autoDiscoveryRegistry = get(),
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
    factory<DisplayOptionsContract.Validator> { DisplayOptionsValidator() }

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
            clientInfoAppName = get(named("ClientInfoAppName")),
            clientInfoAppVersion = get(named("ClientInfoAppVersion")),
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
        DisplayOptionsViewModel(
            validator = get(),
            accountStateRepository = get(),
            accountOwnerNameProvider = get(),
        )
    }

    viewModel {
        SyncOptionsViewModel(
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
