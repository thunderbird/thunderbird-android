package app.k9mail.core.common.net.ssl

import java.security.GeneralSecurityException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import okio.Buffer

/**
 * Decode a PEM encoded certificate [RFC-7468](https://tools.ietf.org/html/rfc7468)
 * into a [X509Certificate].
 *
 * Taken from okhttp3.tls.Certificates
 *
 * @throws IllegalArgumentException if the certificate could not be decoded.
 */
@Suppress("ThrowsCount")
fun String.decodeCertificatePem(): X509Certificate {
    try {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = certificateFactory.generateCertificates(Buffer().writeUtf8(this).inputStream())

        return certificates.single() as X509Certificate
    } catch (exception: NoSuchElementException) {
        throw IllegalArgumentException("failed to decode certificate", exception)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("failed to decode certificate", exception)
    } catch (exception: GeneralSecurityException) {
        throw IllegalArgumentException("failed to decode certificate", exception)
    }
}
