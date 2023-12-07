package com.fsck.k9.mail

import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * A [CertificateException] extension that provides access to the pertinent certificate chain.
 */
class CertificateChainException(
    message: String?,
    val certChain: Array<out X509Certificate>?,
    cause: Throwable?,
) : CertificateException(message, cause)
