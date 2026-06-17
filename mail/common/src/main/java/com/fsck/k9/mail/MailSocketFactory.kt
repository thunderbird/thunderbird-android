package com.fsck.k9.mail

import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.Base64
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
        val socket = connectProxyTunnelSocket(host, port, proxySettings, connectTimeout)

        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
            trustedSocketFactory.createSocket(socket, host, port, clientCertificateAlias)
        } else {
            socket
        }
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
    ): Socket {
        val socket = if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
            trustedSocketFactory.createSocket(null, host, port, clientCertificateAlias)
        } else {
            Socket()
        }

        socket.connect(socketAddress, connectTimeout)
        return socket
    }

    @Throws(IOException::class)
    private fun connectProxyTunnelSocket(
        host: String,
        port: Int,
        proxySettings: MailProxySettings,
        connectTimeout: Int,
    ): Socket {
        val socket = Socket()
        socket.connect(InetSocketAddress(checkNotNull(proxySettings.host), proxySettings.port), connectTimeout)
        socket.soTimeout = connectTimeout

        try {
            when (proxySettings.type) {
                MailProxyType.HTTP -> connectHttpTunnel(socket, host, port, proxySettings)
                MailProxyType.SOCKS4 -> connectSocks4Tunnel(socket, host, port, proxySettings)
                MailProxyType.SOCKS5 -> connectSocks5Tunnel(socket, host, port, proxySettings)
                MailProxyType.NONE -> error("Proxy is disabled")
            }
            return socket
        } catch (e: IOException) {
            socket.close()
            throw e
        }
    }

    @Throws(IOException::class)
    private fun connectHttpTunnel(socket: Socket, host: String, port: Int, proxySettings: MailProxySettings) {
        val output = socket.getOutputStream()
        val request = buildString {
            append("CONNECT ")
            append(formatHostPort(host, port))
            append(" HTTP/1.1\r\nHost: ")
            append(formatHostPort(host, port))
            append("\r\n")
            proxySettings.basicAuthorizationHeader()?.let { append(it) }
            append("\r\n")
        }

        output.write(request.toByteArray(StandardCharsets.ISO_8859_1))
        output.flush()

        val responseHeader = readHttpResponseHeader(socket)
        val statusLine = responseHeader.lineSequence().firstOrNull().orEmpty()
        val statusCode = statusLine.split(' ').getOrNull(1)?.toIntOrNull()
        if (statusCode != HTTP_TUNNEL_SUCCESS) {
            throw IOException("HTTP proxy CONNECT failed: $statusLine")
        }
    }

    private fun MailProxySettings.basicAuthorizationHeader(): String? {
        val proxyUsername = username?.takeIf { it.isNotBlank() } ?: return null
        val proxyPassword = password.orEmpty()
        val credentials = "$proxyUsername:$proxyPassword".toByteArray(StandardCharsets.ISO_8859_1)
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials)

        return "Proxy-Authorization: Basic $encodedCredentials\r\n"
    }

    @Throws(IOException::class)
    private fun readHttpResponseHeader(socket: Socket): String {
        val input = socket.getInputStream()
        val bytes = ArrayList<Byte>()
        var previous = 0
        var current = input.readByteStrict()

        while (bytes.size < MAX_HTTP_RESPONSE_HEADER_BYTES) {
            bytes.add(current.toByte())
            val lastFourBytes = bytes.takeLast(4)
            if (lastFourBytes == HTTP_HEADER_TERMINATOR) {
                return bytes.toByteArray().toString(StandardCharsets.ISO_8859_1)
            }

            previous = current
            current = input.readByteStrict()
            if (previous == '\n'.code && current == -1) break
        }

        throw IOException("HTTP proxy response header is too large")
    }

    @Throws(IOException::class)
    private fun connectSocks4Tunnel(socket: Socket, host: String, port: Int, proxySettings: MailProxySettings) {
        val output = socket.getOutputStream()
        val userId = proxySettings.username.orEmpty().toByteArray(StandardCharsets.UTF_8)
        val request = ArrayList<Byte>()

        request.add(SOCKS4_VERSION)
        request.add(SOCKS_CONNECT_COMMAND)
        request.add((port shr 8).toByte())
        request.add(port.toByte())
        if (proxySettings.proxyDns) {
            request.addAll(SOCKS4A_DOMAIN_SENTINEL.toList())
        } else {
            request.addAll(resolveIpv4Address(host).toList())
        }
        request.addAll(userId.toList())
        request.add(0.toByte())
        if (proxySettings.proxyDns) {
            request.addAll(host.toByteArray(StandardCharsets.UTF_8).toList())
            request.add(0.toByte())
        }

        output.write(request.toByteArray())
        output.flush()

        val input = socket.getInputStream()
        val response = ByteArray(SOCKS4_RESPONSE_LENGTH)
        input.readFully(response)
        if (response[1] != SOCKS4_REQUEST_GRANTED) {
            throw IOException("SOCKS4 proxy CONNECT failed: ${response[1].toInt() and BYTE_MASK}")
        }
    }

    @Throws(IOException::class)
    private fun connectSocks5Tunnel(socket: Socket, host: String, port: Int, proxySettings: MailProxySettings) {
        val output = socket.getOutputStream()
        val input = socket.getInputStream()
        val hasCredentials = !proxySettings.username.isNullOrBlank()
        val methods = if (hasCredentials) byteArrayOf(SOCKS_NO_AUTH_METHOD, SOCKS_USERNAME_PASSWORD_METHOD) else {
            byteArrayOf(SOCKS_NO_AUTH_METHOD)
        }

        output.write(byteArrayOf(SOCKS5_VERSION, methods.size.toByte()))
        output.write(methods)
        output.flush()

        val selectedMethod = readSocksMethodSelection(input)
        if (selectedMethod == SOCKS_USERNAME_PASSWORD_METHOD) {
            authenticateSocks5(output, input, proxySettings)
        } else if (selectedMethod != SOCKS_NO_AUTH_METHOD) {
            throw IOException("SOCKS5 proxy does not accept offered authentication methods")
        }

        output.write(buildSocks5ConnectRequest(host, port, proxySettings.proxyDns))
        output.flush()

        readSocks5ConnectResponse(input)
    }

    @Throws(IOException::class)
    private fun readSocksMethodSelection(input: java.io.InputStream): Byte {
        val version = input.readByteStrict().toByte()
        val method = input.readByteStrict().toByte()
        if (version != SOCKS5_VERSION) {
            throw IOException("SOCKS5 proxy sent invalid greeting response")
        }

        return method
    }

    @Throws(IOException::class)
    private fun authenticateSocks5(
        output: java.io.OutputStream,
        input: java.io.InputStream,
        proxySettings: MailProxySettings,
    ) {
        val username = proxySettings.username.orEmpty().toByteArray(StandardCharsets.UTF_8)
        val password = proxySettings.password.orEmpty().toByteArray(StandardCharsets.UTF_8)
        if (username.size > SOCKS5_MAX_FIELD_LENGTH || password.size > SOCKS5_MAX_FIELD_LENGTH) {
            throw IOException("SOCKS5 username/password is too long")
        }

        output.write(byteArrayOf(SOCKS5_AUTH_VERSION, username.size.toByte()))
        output.write(username)
        output.write(password.size)
        output.write(password)
        output.flush()

        val version = input.readByteStrict().toByte()
        val status = input.readByteStrict().toByte()
        if (version != SOCKS5_AUTH_VERSION || status != SOCKS5_AUTH_SUCCESS) {
            throw IOException("SOCKS5 proxy authentication failed")
        }
    }

    @Throws(IOException::class)
    private fun buildSocks5ConnectRequest(host: String, port: Int, proxyDns: Boolean): ByteArray {
        val request = ArrayList<Byte>()
        request.add(SOCKS5_VERSION)
        request.add(SOCKS_CONNECT_COMMAND)
        request.add(SOCKS_RESERVED)

        if (proxyDns) {
            val hostBytes = host.toByteArray(StandardCharsets.UTF_8)
            if (hostBytes.size > SOCKS5_MAX_FIELD_LENGTH) {
                throw IOException("SOCKS5 destination host is too long")
            }
            request.add(SOCKS_ADDRESS_DOMAIN)
            request.add(hostBytes.size.toByte())
            request.addAll(hostBytes.toList())
        } else {
            val address = InetAddress.getByName(host)
            when (address.address.size) {
                IPV4_LENGTH -> request.add(SOCKS_ADDRESS_IPV4)
                IPV6_LENGTH -> request.add(SOCKS_ADDRESS_IPV6)
                else -> throw IOException("Unsupported destination address length")
            }
            request.addAll(address.address.toList())
        }

        request.add((port shr 8).toByte())
        request.add(port.toByte())

        return request.toByteArray()
    }

    @Throws(IOException::class)
    private fun readSocks5ConnectResponse(input: java.io.InputStream) {
        val version = input.readByteStrict().toByte()
        val status = input.readByteStrict().toByte()
        input.readByteStrict()
        val addressType = input.readByteStrict().toByte()

        if (version != SOCKS5_VERSION) {
            throw IOException("SOCKS5 proxy sent invalid CONNECT response")
        }
        if (status != SOCKS5_SUCCESS) {
            throw IOException("SOCKS5 proxy CONNECT failed: ${status.toInt() and BYTE_MASK}")
        }

        val addressLength = when (addressType) {
            SOCKS_ADDRESS_IPV4 -> IPV4_LENGTH
            SOCKS_ADDRESS_IPV6 -> IPV6_LENGTH
            SOCKS_ADDRESS_DOMAIN -> input.readByteStrict()
            else -> throw IOException("SOCKS5 proxy sent unsupported address type")
        }
        input.skipFully(addressLength + PORT_LENGTH)
    }

    @Throws(IOException::class)
    private fun resolveIpv4Address(host: String): ByteArray {
        val address = InetAddress.getByName(host).address
        if (address.size != IPV4_LENGTH) {
            throw IOException("SOCKS4 proxy requires an IPv4 destination address")
        }

        return address
    }

    private fun formatHostPort(host: String, port: Int): String {
        val formattedHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        return "$formattedHost:$port"
    }

    @Throws(IOException::class)
    private fun java.io.InputStream.readByteStrict(): Int {
        val value = read()
        if (value == -1) throw EOFException("Proxy closed the connection")
        return value
    }

    @Throws(IOException::class)
    private fun java.io.InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val count = read(buffer, offset, buffer.size - offset)
            if (count == -1) throw EOFException("Proxy closed the connection")
            offset += count
        }
    }

    @Throws(IOException::class)
    private fun java.io.InputStream.skipFully(length: Int) {
        var remaining = length
        val buffer = ByteArray(1024)
        while (remaining > 0) {
            val count = read(buffer, 0, minOf(buffer.size, remaining))
            if (count == -1) throw EOFException("Proxy closed the connection")
            remaining -= count
        }
    }

    private const val BYTE_MASK = 0xff
    private const val HTTP_TUNNEL_SUCCESS = 200
    private const val MAX_HTTP_RESPONSE_HEADER_BYTES = 32 * 1024
    private val HTTP_HEADER_TERMINATOR = listOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
    private const val PORT_LENGTH = 2
    private const val IPV4_LENGTH = 4
    private const val IPV6_LENGTH = 16
    private const val SOCKS_CONNECT_COMMAND: Byte = 0x01
    private const val SOCKS_RESERVED: Byte = 0x00
    private const val SOCKS_NO_AUTH_METHOD: Byte = 0x00
    private const val SOCKS_USERNAME_PASSWORD_METHOD: Byte = 0x02
    private const val SOCKS4_VERSION: Byte = 0x04
    private const val SOCKS4_RESPONSE_LENGTH = 8
    private const val SOCKS4_REQUEST_GRANTED: Byte = 0x5a
    private val SOCKS4A_DOMAIN_SENTINEL = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    private const val SOCKS5_VERSION: Byte = 0x05
    private const val SOCKS5_AUTH_VERSION: Byte = 0x01
    private const val SOCKS5_AUTH_SUCCESS: Byte = 0x00
    private const val SOCKS5_SUCCESS: Byte = 0x00
    private const val SOCKS5_MAX_FIELD_LENGTH = 255
    private const val SOCKS_ADDRESS_IPV4: Byte = 0x01
    private const val SOCKS_ADDRESS_DOMAIN: Byte = 0x03
    private const val SOCKS_ADDRESS_IPV6: Byte = 0x04
}
