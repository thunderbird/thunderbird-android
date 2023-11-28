package com.fsck.k9.mail

/**
 * Thrown when there's a problem with the client certificate used with TLS.
 */
class ClientCertificateException(
    val error: ClientCertificateError,
    cause: Throwable,
) : MessagingException("Problem with client certificate: $error", true, cause)

enum class ClientCertificateError {
    RetrievalFailure,
    CertificateExpired,
}
