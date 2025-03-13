package com.fsck.k9.mail.testing.security

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * A test trusted socket factory that creates sockets that trust only a predefined server certificate
 */
object TestTrustedSocketFactory : TrustedSocketFactory {

    private val serverCertificate: X509Certificate by lazy {
        KeyStoreProvider.instance.serverCertificate
    }

    @Throws(
        NoSuchAlgorithmException::class,
        KeyManagementException::class,
        MessagingException::class,
        IOException::class,
    )
    override fun createSocket(
        socket: Socket?,
        host: String,
        port: Int,
        clientCertificateAlias: String?,
    ): Socket {
        val trustManagers: Array<TrustManager> = arrayOf(VeryTrustingTrustManager(serverCertificate))

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagers, null)
        }

        val sslSocketFactory = sslContext.socketFactory

        return if (socket == null) {
            sslSocketFactory.createSocket(host, port)
        } else {
            sslSocketFactory.createSocket(
                socket,
                socket.inetAddress.hostAddress,
                socket.port,
                true,
            )
        }
    }
}
