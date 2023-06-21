package com.fsck.k9.mail.helpers

import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SimpleTrustedSocketFactory(private val trustManager: X509TrustManager) : TrustedSocketFactory {
    override fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket {
        requireNotNull(socket)

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
}
