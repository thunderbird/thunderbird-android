package app.k9mail.feature.account.servercertificate.domain

import app.k9mail.feature.account.servercertificate.domain.entity.ServerCertificateError
import java.security.cert.X509Certificate

interface AccountServerCertificateDomainContract {

    interface ServerCertificateErrorRepository {
        fun getCertificateError(): ServerCertificateError?

        fun setCertificateError(serverCertificateError: ServerCertificateError)

        fun clearCertificateError()
    }

    interface UseCase {
        fun interface AddServerCertificateException {
            suspend fun addCertificate(hostname: String, port: Int, certificate: X509Certificate?)
        }
    }
}
