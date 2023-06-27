package app.k9mail.feature.account.setup

import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.service.RealAutoDiscoveryService
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.CheckIncomingServerConfig
import app.k9mail.feature.account.setup.domain.usecase.CheckOutgoingServerConfig
import app.k9mail.feature.account.setup.domain.usecase.GetAutoDiscovery
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
import com.fsck.k9.mail.store.imap.ImapServerSettingsValidator
import com.fsck.k9.mail.store.pop3.Pop3ServerSettingsValidator
import com.fsck.k9.mail.transport.smtp.SmtpServerSettingsValidator
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountSetupModule: Module = module {
    single<AutoDiscoveryService> {
        RealAutoDiscoveryService(
            okHttpClient = get(),
        )
    }

    single<DomainContract.UseCase.GetAutoDiscovery> {
        GetAutoDiscovery(
            service = get(),
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

    factory<AccountAutoDiscoveryContract.Validator> { AccountAutoDiscoveryValidator() }
    factory<AccountIncomingConfigContract.Validator> { AccountIncomingConfigValidator() }
    factory<AccountOutgoingConfigContract.Validator> { AccountOutgoingConfigValidator() }
    factory<AccountOptionsContract.Validator> { AccountOptionsValidator() }

    viewModel { AccountSetupViewModel() }
    viewModel {
        AccountAutoDiscoveryViewModel(
            validator = get(),
            getAutoDiscovery = get(),
        )
    }
    viewModel {
        AccountIncomingConfigViewModel(
            validator = get(),
            checkIncomingServerConfig = get(),
        )
    }
    viewModel {
        AccountOutgoingConfigViewModel(
            validator = get(),
            checkOutgoingServerConfig = get(),
        )
    }
    viewModel {
        AccountOptionsViewModel(
            validator = get(),
        )
    }
}
