package com.fsck.k9.mail

import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketAddress
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import net.thunderbird.core.common.exception.MessagingException

object MailSocketFactory {
    @JvmStatic
    @Throws(IOException::class, MessagingException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    fun connectSocket(
        host: String,
        port: Int,
        connectionSecurity: ConnectionSecurity,
        clientCertificateAlias: String?,
        trustedSocketFactory: TrustedSocketFactory,
        proxySettings: MailProxySettings,
        connectTimeout: Int,
    ): Socket {
        return if (proxySettings.type == MailProxyType.NONE) {
            connectDirectSocket(host, port, connectionSecurity, clientCertificateAlias, trustedSocketFactory, connectTimeout)
        } else {
            connectProxySocket(
                host = host,
                port = port,
                connectionSecurity = connectionSecurity,
                clientCertificateAlias = clientCertificateAlias,
                trustedSocketFactory = trustedSocketFactory,
                proxySettings = proxySettings,
                connectTimeout = connectTimeout,
            )
        }
    }

    @Throws(IOException::class, MessagingException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun connectDirectSocket(
        host: String,
        port: Int,
        connectionSecurity: ConnectionSecurity,
        clientCertificateAlias: String?,
        trustedSocketFactory: TrustedSocketFactory,
        connectTimeout: Int,
    ): Socket {
        val inetAddresses = InetAddress.getAllByName(host)

        var connectException: Exception? = null
        for (address in inetAddresses) {
            connectException = try {
                val socketAddress = InetSocketAddress(address, port)
                return createAndConnectSocket(
                    socketAddress = socketAddress,
                    connectionSecurity = connectionSecurity,
                    host = host,
                    port = port,
                    clientCertificateAlias = clientCertificateAlias,
                    trustedSocketFactory = trustedSocketFactory,
                    connectTimeout = connectTimeout,
                    proxy = null,
                )
            } catch (e: IOException) {
                e
            }
        }

        throw connectException ?: UnknownHostException()
    }

    @Throws(IOException::class, MessagingException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun connectProxySocket(
        host: String,
        port: Int,
        connectionSecurity: ConnectionSecurity,
        clientCertificateAlias: String?,
        trustedSocketFactory: TrustedSocketFactory,
        proxySettings: MailProxySettings,
        connectTimeout: Int,
    ): Socket {
        val proxyHost = checkNotNull(proxySettings.host)
        val proxyAddress = InetSocketAddress(proxyHost, proxySettings.port)
        val proxy = Proxy(proxySettings.toJavaProxyType(), proxyAddress)
        val socketAddress = if (proxySettings.proxyDns) {
            InetSocketAddress.createUnresolved(host, port)
        } else {
            InetSocketAddress(host, port)
        }

        return createAndConnectSocket(
            socketAddress = socketAddress,
            connectionSecurity = connectionSecurity,
            host = host,
            port = port,
            clientCertificateAlias = clientCertificateAlias,
            trustedSocketFactory = trustedSocketFactory,
            connectTimeout = connectTimeout,
            proxy = proxy,
        )
    }

    @Throws(IOException::class, MessagingException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun createAndConnectSocket(
        socketAddress: SocketAddress,
        connectionSecurity: ConnectionSecurity,
        host: String,
        port: Int,
        clientCertificateAlias: String?,
        trustedSocketFactory: TrustedSocketFactory,
        connectTimeout: Int,
        proxy: Proxy?,
    ): Socket {
        val socket = if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED && proxy == null) {
            trustedSocketFactory.createSocket(null, host, port, clientCertificateAlias)
        } else {
            proxy?.let(::Socket) ?: Socket()
        }

        socket.connect(socketAddress, connectTimeout)

        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED && proxy != null) {
            trustedSocketFactory.createSocket(socket, host, port, clientCertificateAlias)
        } else {
            socket
        }
    }

    private fun MailProxySettings.toJavaProxyType(): Proxy.Type {
        return when (type) {
            MailProxyType.HTTP -> Proxy.Type.HTTP
            MailProxyType.SOCKS -> Proxy.Type.SOCKS
            MailProxyType.NONE -> error("Proxy is disabled")
        }
    }
}
