package app.k9mail.core.common.net.ssl

import java.security.cert.X509Certificate

interface TrustedCertificateProvider {
    fun getCertificates(): List<X509Certificate>
}
