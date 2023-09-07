package app.k9mail.feature.account.server.certificate.data

import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError

class InMemoryServerCertificateErrorRepository(
    private var serverCertificateError: ServerCertificateError? = null,
) : ServerCertificateDomainContract.ServerCertificateErrorRepository {

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
