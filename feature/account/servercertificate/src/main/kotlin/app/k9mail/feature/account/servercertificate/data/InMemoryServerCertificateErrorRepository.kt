package app.k9mail.feature.account.servercertificate.data

import app.k9mail.feature.account.servercertificate.domain.AccountServerCertificateDomainContract
import app.k9mail.feature.account.servercertificate.domain.entity.ServerCertificateError

class InMemoryServerCertificateErrorRepository(
    private var serverCertificateError: ServerCertificateError? = null,
) : AccountServerCertificateDomainContract.ServerCertificateErrorRepository {

    override fun getCertificateError(): ServerCertificateError? {
        return serverCertificateError
    }

    override fun setCertificateError(serverCertificateError: ServerCertificateError) {
        this.serverCertificateError = serverCertificateError
    }

    override fun clearCertificateError() {
        serverCertificateError = null
    }
}
