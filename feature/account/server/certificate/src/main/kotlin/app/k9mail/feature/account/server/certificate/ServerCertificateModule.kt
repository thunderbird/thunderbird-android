package app.k9mail.feature.account.server.certificate

import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.usecase.AddServerCertificateException
import app.k9mail.feature.account.server.certificate.domain.usecase.FormatServerCertificateError
import app.k9mail.feature.account.server.certificate.ui.DefaultFingerprintFormatter
import app.k9mail.feature.account.server.certificate.ui.DefaultServerNameFormatter
import app.k9mail.feature.account.server.certificate.ui.FingerprintFormatter
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorViewModel
import app.k9mail.feature.account.server.certificate.ui.ServerNameFormatter
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountServerCertificateModule: Module = module {

    single<ServerCertificateDomainContract.ServerCertificateErrorRepository> {
        InMemoryServerCertificateErrorRepository()
    }

    factory<ServerCertificateDomainContract.UseCase.AddServerCertificateException> {
        AddServerCertificateException(
            localKeyStore = get(),
        )
    }

    factory<ServerCertificateDomainContract.UseCase.FormatServerCertificateError> {
        FormatServerCertificateError()
    }

    factory<ServerNameFormatter> { DefaultServerNameFormatter() }

    factory<FingerprintFormatter> { DefaultFingerprintFormatter() }

    viewModel {
        ServerCertificateErrorViewModel(
            certificateErrorRepository = get(),
            addServerCertificateException = get(),
            formatServerCertificateError = get(),
        )
    }
}
