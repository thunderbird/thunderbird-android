package app.k9mail.feature.account.setup

import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.service.RealAutoDiscoveryService
import app.k9mail.core.common.coreCommonModule
import app.k9mail.feature.account.oauth.featureAccountOAuthModule
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.CreateAccount
import app.k9mail.feature.account.setup.domain.usecase.GetAutoDiscovery
import app.k9mail.feature.account.setup.domain.usecase.ValidateServerSettings
import app.k9mail.feature.account.setup.ui.AccountSetupViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryValidator
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigValidator
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsValidator
import app.k9mail.feature.account.setup.ui.options.AccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigValidator
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigViewModel
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import app.k9mail.feature.account.setup.ui.validation.AccountValidationViewModel
import app.k9mail.feature.account.setup.ui.validation.InMemoryAuthStateStorage
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.store.imap.ImapServerSettingsValidator
import com.fsck.k9.mail.store.pop3.Pop3ServerSettingsValidator
import com.fsck.k9.mail.transport.smtp.SmtpServerSettingsValidator
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureAccountSetupModule: Module = module {
    includes(coreCommonModule, featureAccountOAuthModule)

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

    factory<DomainContract.UseCase.ValidateServerSettings> { (authStateStorage: AuthStateStorage) ->
        ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = ImapServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProviderFactory = get(),
                clientIdAppName = "null",
            ),
            pop3Validator = Pop3ServerSettingsValidator(
                trustedSocketFactory = get(),
            ),
            smtpValidator = SmtpServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProviderFactory = get(),
            ),
        )
    }

    factory<DomainContract.UseCase.CreateAccount> {
        CreateAccount(
            accountCreator = get(),
        )
    }

    factory<AccountAutoDiscoveryContract.Validator> { AccountAutoDiscoveryValidator() }
    factory<AccountIncomingConfigContract.Validator> { AccountIncomingConfigValidator() }
    factory<AccountOutgoingConfigContract.Validator> { AccountOutgoingConfigValidator() }
    factory<AccountOptionsContract.Validator> { AccountOptionsValidator() }

    viewModel {
        val authStateStorage = InMemoryAuthStateStorage()

        AccountSetupViewModel(
            createAccount = get(),
            autoDiscoveryViewModel = get(),
            incomingViewModel = get(),
            incomingValidationViewModel = get(named(NAME_INCOMING_VALIDATION)) { parametersOf(authStateStorage) },
            outgoingViewModel = get(),
            outgoingValidationViewModel = get(named(NAME_OUTGOING_VALIDATION)) { parametersOf(authStateStorage) },
            optionsViewModel = get(),
            authStateStorage = authStateStorage,
        )
    }
    factory<AccountAutoDiscoveryContract.ViewModel> {
        AccountAutoDiscoveryViewModel(
            validator = get(),
            getAutoDiscovery = get(),
            oAuthViewModel = get(),
        )
    }
    factory<AccountIncomingConfigContract.ViewModel> {
        AccountIncomingConfigViewModel(
            validator = get(),
        )
    }
    factory<AccountValidationContract.ViewModel>(named(NAME_INCOMING_VALIDATION)) {
            (authStateStorage: AuthStateStorage) ->

        AccountValidationViewModel(
            validateServerSettings = get { parametersOf(authStateStorage) },
            initialState = AccountValidationContract.State(
                isIncomingValidation = true,
            ),
        )
    }
    factory<AccountOutgoingConfigContract.ViewModel> {
        AccountOutgoingConfigViewModel(
            validator = get(),
        )
    }
    factory<AccountValidationContract.ViewModel>(named(NAME_OUTGOING_VALIDATION)) {
            (authStateStorage: AuthStateStorage) ->

        AccountValidationViewModel(
            validateServerSettings = get { parametersOf(authStateStorage) },
            initialState = AccountValidationContract.State(
                isIncomingValidation = false,
            ),
        )
    }
    factory<AccountOptionsContract.ViewModel> {
        AccountOptionsViewModel(
            validator = get(),
        )
    }
}

internal const val NAME_INCOMING_VALIDATION = "incoming_validation"
internal const val NAME_OUTGOING_VALIDATION = "outgoing_validation"
