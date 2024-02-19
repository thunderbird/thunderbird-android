package app.k9mail.core.common.net.ssl

import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates

fun OkHttpClient.Builder.installTrustedCertificates(
    trustedCertificateProvider: TrustedCertificateProvider,
): OkHttpClient.Builder {
    val handshakeCertificates = HandshakeCertificates.Builder().apply {
        trustedCertificateProvider.getCertificates().forEach { addTrustedCertificate(it) }

        addPlatformTrustedCertificates()
    }.build()

    sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager)

    return this
}
