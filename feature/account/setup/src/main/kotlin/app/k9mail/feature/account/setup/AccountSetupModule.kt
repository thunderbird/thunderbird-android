package app.k9mail.feature.account.setup

import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.service.RealAutoDiscoveryService
import app.k9mail.core.common.coreCommonModule
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.CheckIncomingServerConfig
import app.k9mail.feature.account.setup.domain.usecase.CheckOutgoingServerConfig
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
import com.fsck.k9.mail.store.imap.ImapServerSettingsValidator
import com.fsck.k9.mail.store.pop3.Pop3ServerSettingsValidator
import com.fsck.k9.mail.transport.smtp.SmtpServerSettingsValidator
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureAccountSetupModule: Module = module {
    includes(coreCommonModule)

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

    factory<DomainContract.UseCase.ValidateServerSettings> {
        ValidateServerSettings(
            imapValidator = ImapServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProvider = null, // TODO
                clientIdAppName = "null",
            ),
            pop3Validator = Pop3ServerSettingsValidator(
                trustedSocketFactory = get(),
            ),
            smtpValidator = SmtpServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProvider = null, // TODO
            ),
        )
    }

    factory<DomainContract.UseCase.CheckIncomingServerConfig> {
        CheckIncomingServerConfig(
            imapValidator = ImapServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProvider = null,
                clientIdAppName = "null",
            ),
            pop3Validator = Pop3ServerSettingsValidator(
                trustedSocketFactory = get(),
            ),
        )
    }
    factory<DomainContract.UseCase.CheckOutgoingServerConfig> {
        CheckOutgoingServerConfig(
            smtpValidator = SmtpServerSettingsValidator(
                trustedSocketFactory = get(),
                oAuth2TokenProvider = null,
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
        AccountSetupViewModel(
            createAccount = get(),
        )
    }
    viewModel {
        AccountAutoDiscoveryViewModel(
            validator = get(),
            getAutoDiscovery = get(),
        )
    }
    viewModel {
        AccountIncomingConfigViewModel(
            validator = get(),
        )
    }
    viewModel(named(NAME_INCOMING_VALIDATION)) {
        AccountValidationViewModel(
            validateServerSettings = get(),
            initialState = AccountValidationContract.State(
                isIncomingValidation = true,
            ),
        )
    }
    viewModel {
        AccountOutgoingConfigViewModel(
            validator = get(),
        )
    }
    viewModel(named(NAME_OUTGOING_VALIDATION)) {
        AccountValidationViewModel(
            validateServerSettings = get(),
            initialState = AccountValidationContract.State(
                isIncomingValidation = false,
            ),
        )
    }
    viewModel {
        AccountOptionsViewModel(
            validator = get(),
        )
    }
}

internal const val NAME_INCOMING_VALIDATION = "incoming_validation"
internal const val NAME_OUTGOING_VALIDATION = "outgoing_validation"
