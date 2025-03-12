package com.fsck.k9.helper

import android.content.Context
import android.net.SSLCertificateSocketFactory
import android.os.Build
import android.text.TextUtils
import app.k9mail.core.common.net.HostNameUtils.isLegalIPAddress
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.Collections
import javax.net.ssl.KeyManager
import javax.net.ssl.SNIHostName
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import timber.log.Timber

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

    companion object {
        private val ENABLED_CIPHERS: Array<String?>?
        private val ENABLED_PROTOCOLS: Array<String?>?

        private val DISALLOWED_CIPHERS = arrayOf<String?>(
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

        private val DISALLOWED_PROTOCOLS = arrayOf<String?>(
            "SSLv3",
        )

        init {
            var enabledCiphers: Array<String?>? = null
            var supportedProtocols: Array<String?>? = null

            try {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, null)
                val sf = sslContext.socketFactory
                val sock = sf.createSocket() as SSLSocket
                enabledCiphers = sock.enabledCipherSuites

                /*
                 * Retrieve all supported protocols, not just the (default) enabled
                 * ones. TLSv1.1 & TLSv1.2 are supported on API levels 16+, but are
                 * only enabled by default on API levels 20+.
                 */
                supportedProtocols = sock.supportedProtocols
            } catch (e: Exception) {
                Timber.e(e, "Error getting information about available SSL/TLS ciphers and protocols")
            }

            ENABLED_CIPHERS = if (enabledCiphers == null) null else remove(enabledCiphers, DISALLOWED_CIPHERS)
            ENABLED_PROTOCOLS =
                if (supportedProtocols == null) null else remove(supportedProtocols, DISALLOWED_PROTOCOLS)
        }

        private fun remove(enabled: Array<String?>, disallowed: Array<String?>?): Array<String?> {
            val items: MutableList<String?> = ArrayList<String?>()
            Collections.addAll<String?>(items, *enabled)

            if (disallowed != null) {
                for (item in disallowed) {
                    items.remove(item)
                }
            }

            return items.toTypedArray<String?>()
        }

        private fun hardenSocket(sock: SSLSocket) {
            if (ENABLED_CIPHERS != null) {
                sock.enabledCipherSuites = ENABLED_CIPHERS
            }
            if (ENABLED_PROTOCOLS != null) {
                sock.enabledProtocols = ENABLED_PROTOCOLS
            }
        }

        private fun setSniHost(factory: SSLSocketFactory?, socket: SSLSocket, hostname: String?) {
            if (factory is SSLCertificateSocketFactory) {
                val sslCertificateSocketFactory = factory
                sslCertificateSocketFactory.setHostname(socket, hostname)
            } else if (Build.VERSION.SDK_INT >= 24) {
                val sslParameters = socket.getSSLParameters()
                val sniServerNames = mutableListOf<SNIServerName?>(SNIHostName(hostname))
                sslParameters.serverNames = sniServerNames
                socket.setSSLParameters(sslParameters)
            } else {
                setHostnameViaReflection(socket, hostname)
            }
        }

        private fun setHostnameViaReflection(socket: SSLSocket, hostname: String?) {
            try {
                socket.javaClass.getMethod("setHostname", String::class.java).invoke(socket, hostname)
            } catch (e: Throwable) {
                Timber.e(e, "Could not call SSLSocket#setHostname(String) method ")
            }
        }
    }
}
