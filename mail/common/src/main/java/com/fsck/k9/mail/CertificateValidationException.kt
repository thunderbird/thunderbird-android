package com.fsck.k9.mail

import java.security.cert.X509Certificate
import net.thunderbird.core.common.exception.MessagingException

class CertificateValidationException(
    val certificateChain: List<X509Certificate>,
    cause: Throwable?,
) : MessagingException(cause)
