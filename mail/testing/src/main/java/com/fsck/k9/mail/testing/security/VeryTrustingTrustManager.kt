package com.fsck.k9.mail.testing.security

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * A very trusting trust manager that accepts all certificates. It is used in tests to accept all certificates.
 *
 * WARNING: This trust manager is very insecure and should never be used in production code!
 *
 * @param serverCertificate The server certificate to return as the accepted issuer.
 */
@Suppress("CustomX509TrustManager")
internal class VeryTrustingTrustManager(private val serverCertificate: X509Certificate?) : X509TrustManager {

    /**
     * Always trust the client certificate.
     */
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) = Unit

    /**
     * Always trust the server certificate.
     */
    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOf<X509Certificate?>(serverCertificate)
    }
}
