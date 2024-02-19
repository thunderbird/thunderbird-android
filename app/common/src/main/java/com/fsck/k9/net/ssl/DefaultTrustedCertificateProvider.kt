package com.fsck.k9.net.ssl

import android.os.Build
import app.k9mail.core.common.net.ssl.TrustedCertificateProvider
import app.k9mail.core.common.net.ssl.decodeCertificatePem
import java.security.cert.X509Certificate

class DefaultTrustedCertificateProvider : TrustedCertificateProvider {
    override fun getCertificates(): List<X509Certificate> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            listOf(
                TrustedCertificates.certificateIsrgRootX1.decodeCertificatePem(),
                TrustedCertificates.certificateIsrgRootX2.decodeCertificatePem(),
            )
        } else {
            emptyList()
        }
    }
}
