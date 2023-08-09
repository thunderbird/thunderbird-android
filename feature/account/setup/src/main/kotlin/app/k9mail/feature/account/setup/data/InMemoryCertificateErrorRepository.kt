package app.k9mail.feature.account.setup.data

import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.CertificateError

class InMemoryCertificateErrorRepository(
    private var certificateError: CertificateError? = null,
) : DomainContract.CertificateErrorRepository {

    override fun getCertificateError(): CertificateError? {
        return certificateError
    }

    override fun setCertificateError(certificateError: CertificateError) {
        this.certificateError = certificateError
    }

    override fun clearCertificateError() {
        certificateError = null
    }
}
