package com.fsck.k9.mail.ssl

import com.fsck.k9.mail.CertificateChainException
import java.security.cert.X509Certificate

/**
 * Checks if an exception chain contains a [CertificateChainException] and if so, extracts the certificate chain from it
 */
object CertificateChainExtractor {
    @JvmStatic
    fun extract(throwable: Throwable): List<X509Certificate>? {
        return findCertificateChainException(throwable)?.certChain?.toList()
    }

    private tailrec fun findCertificateChainException(throwable: Throwable): CertificateChainException? {
        val cause = throwable.cause
        return when {
            throwable is CertificateChainException -> throwable
            cause == null -> null
            else -> findCertificateChainException(cause)
        }
    }
}
