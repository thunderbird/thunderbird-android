package com.fsck.k9.mail.testing.security

import com.fsck.k9.mail.ClientCertificateError
import com.fsck.k9.mail.ClientCertificateException
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SimpleTrustedSocketFactory(private val trustManager: X509TrustManager) : TrustedSocketFactory {
    private var clientCertificateError: ClientCertificateError? = null

    override fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket {
        requireNotNull(socket)

        @Suppress("ThrowingExceptionsWithoutMessageOrCause")
        when (val error = clientCertificateError) {
            ClientCertificateError.RetrievalFailure -> throw ClientCertificateException(error, RuntimeException())
            ClientCertificateError.CertificateExpired -> throw ClientCertificateException(error, RuntimeException())
            null -> Unit
        }

        val trustManagers = arrayOf<TrustManager>(trustManager)

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagers, null)
        }

        return sslContext.socketFactory.createSocket(
            socket,
            socket.inetAddress.hostAddress,
            socket.port,
            true,
        )
    }

    fun injectClientCertificateError(error: ClientCertificateError) {
        clientCertificateError = error
    }
}
