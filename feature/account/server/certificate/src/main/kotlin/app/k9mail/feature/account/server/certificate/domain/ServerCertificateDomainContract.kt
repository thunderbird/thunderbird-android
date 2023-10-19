package app.k9mail.feature.account.server.certificate.domain

import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import java.security.cert.X509Certificate

interface ServerCertificateDomainContract {

    interface ServerCertificateErrorRepository {
        fun getCertificateError(): ServerCertificateError?

        fun setCertificateError(serverCertificateError: ServerCertificateError)

        fun clearCertificateError()
    }

    interface UseCase {
        fun interface AddServerCertificateException {
            suspend fun addCertificate(hostname: String, port: Int, certificate: X509Certificate?)
        }

        fun interface FormatServerCertificateError {
            operator fun invoke(serverCertificateError: ServerCertificateError): FormattedServerCertificateError
        }
    }
}
