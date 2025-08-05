package com.fsck.k9.helper

import android.content.Context
import android.net.SSLCertificateSocketFactory
import android.os.Build
import android.text.TextUtils
import net.thunderbird.core.common.exception.MessagingException
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.KeyManager
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import net.thunderbird.core.common.net.HostNameUtils.isLegalIPAddress
import net.thunderbird.core.logging.legacy.Log

class DefaultTrustedSocketFactory(
    private val context: Context?,
    private val trustManagerFactory: TrustManagerFactory,
) : TrustedSocketFactory {

    @Throws(
        NoSuchAlgorithmException::class,
        KeyManagementException::class,
        MessagingException::class,
        IOException::class,
    )
    override fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket {
        val trustManagers = arrayOf<TrustManager?>(trustManagerFactory.getTrustManagerForDomain(host, port))
        var keyManagers: Array<KeyManager?>? = null
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            keyManagers = arrayOf<KeyManager?>(KeyChainKeyManager(context, clientCertificateAlias))
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustManagers, null)
        val socketFactory = sslContext.socketFactory
        val trustedSocket: Socket?
        if (socket == null) {
            trustedSocket = socketFactory.createSocket()
        } else {
            trustedSocket = socketFactory.createSocket(socket, host, port, true)
        }

        val sslSocket = trustedSocket as SSLSocket

        hardenSocket(sslSocket)

        // RFC 6066 does not permit the use of literal IPv4 or IPv6 addresses as SNI hostnames.
        if (isLegalIPAddress(host) == null) {
            setSniHost(socketFactory, sslSocket, host)
        }

        return trustedSocket
    }

    private fun hardenSocket(sock: SSLSocket) {
        ENABLED_CIPHERS?.let { sock.enabledCipherSuites = it }
        ENABLED_PROTOCOLS?.let { sock.enabledProtocols = it }
    }

    @Suppress("TooGenericExceptionCaught")
    companion object {
        private val DISALLOWED_CIPHERS = arrayOf<String>(
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
            "TLS_ECDH_RSA_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_anon_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_NULL_SHA256",
        )

        private val DISALLOWED_PROTOCOLS = arrayOf<String>(
            "SSLv3",
        )

        private val ENABLED_CIPHERS: Array<String>?
        private val ENABLED_PROTOCOLS: Array<String>?

        init {
            var enabledCiphers: Array<String>? = null
            var supportedProtocols: Array<String>? = null

            try {
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, null, null)
                }
                val socket = sslContext.socketFactory.createSocket() as SSLSocket
                enabledCiphers = socket.enabledCipherSuites

                /*
                 * Retrieve all supported protocols, not just the (default) enabled
                 * ones. TLSv1.1 & TLSv1.2 are supported on API levels 16+, but are
                 * only enabled by default on API levels 20+.
                 */
                supportedProtocols = socket.supportedProtocols
            } catch (e: Exception) {
                Log.e(e, "Error getting information about available SSL/TLS ciphers and protocols")
            }

            ENABLED_CIPHERS = enabledCiphers?.let { remove(it, DISALLOWED_CIPHERS) }
            ENABLED_PROTOCOLS = supportedProtocols?.let { remove(it, DISALLOWED_PROTOCOLS) }
        }

        private fun remove(enabled: Array<String>, disallowed: Array<String>): Array<String> {
            return enabled.filterNot { it in disallowed }.toTypedArray()
        }

        private fun setSniHost(factory: SSLSocketFactory, socket: SSLSocket, hostname: String) {
            when {
                factory is SSLCertificateSocketFactory -> factory.setHostname(socket, hostname)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    val sslParameters = socket.sslParameters
                    sslParameters.serverNames = listOf(SNIHostName(hostname))
                    socket.sslParameters = sslParameters
                }

                else -> setHostnameViaReflection(socket, hostname)
            }
        }

        private fun setHostnameViaReflection(socket: SSLSocket, hostname: String?) {
            try {
                socket.javaClass.getMethod("setHostname", String::class.java).invoke(socket, hostname)
            } catch (e: Throwable) {
                Log.e(e, "Could not call SSLSocket#setHostname(String) method ")
            }
        }
    }
}
