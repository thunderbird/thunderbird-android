package app.k9mail.feature.account.servercertificate

import app.k9mail.feature.account.servercertificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.servercertificate.domain.AccountServerCertificateDomainContract
import app.k9mail.feature.account.servercertificate.domain.usecase.AddServerCertificateException
import app.k9mail.feature.account.servercertificate.ui.AccountServerCertificateErrorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountServerCertificateModule: Module = module {

    single<AccountServerCertificateDomainContract.ServerCertificateErrorRepository> {
        InMemoryServerCertificateErrorRepository()
    }

    factory<AccountServerCertificateDomainContract.UseCase.AddServerCertificateException> {
        AddServerCertificateException(
            localKeyStore = get(),
        )
    }

    viewModel {
        AccountServerCertificateErrorViewModel(
            certificateErrorRepository = get(),
            addServerCertificateException = get(),
        )
    }
}
