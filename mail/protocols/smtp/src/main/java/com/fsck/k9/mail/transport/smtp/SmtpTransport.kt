package com.fsck.k9.mail.transport.smtp

import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.Authentication
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.NetworkTimeouts.SOCKET_CONNECT_TIMEOUT
import com.fsck.k9.mail.NetworkTimeouts.SOCKET_READ_TIMEOUT
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import com.fsck.k9.mail.filter.LineWrapOutputStream
import com.fsck.k9.mail.filter.PeekableInputStream
import com.fsck.k9.mail.filter.SmtpDataStuffing
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.XOAuth2ChallengeParser
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.transport.smtp.SmtpHelloResponse.Hello
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.security.GeneralSecurityException
import java.util.Locale
import javax.net.ssl.SSLException
import org.apache.commons.io.IOUtils

private const val SMTP_CONTINUE_REQUEST = 334
private const val SMTP_AUTHENTICATION_FAILURE_ERROR_CODE = 535

class SmtpTransport(
    serverSettings: ServerSettings,
    private val trustedSocketFactory: TrustedSocketFactory,
    private val oauthTokenProvider: OAuth2TokenProvider?
) : Transport() {
    private val host = serverSettings.host
    private val port = serverSettings.port
    private val username = serverSettings.username
    private val password = serverSettings.password
    private val clientCertificateAlias = serverSettings.clientCertificateAlias
    private val authType = serverSettings.authenticationType
    private val connectionSecurity = serverSettings.connectionSecurity

    private var socket: Socket? = null
    private var inputStream: PeekableInputStream? = null
    private var outputStream: OutputStream? = null
    private var responseParser: SmtpResponseParser? = null
    private var is8bitEncodingAllowed = false
    private var isEnhancedStatusCodesProvided = false
    private var largestAcceptableMessage = 0
    private var retryXoauthWithNewToken = false
    private var isPipeliningSupported = false

    private val logger: SmtpLogger = object : SmtpLogger {
        override val isRawProtocolLoggingEnabled: Boolean
            get() = K9MailLib.isDebug()

        override fun log(throwable: Throwable?, message: String, vararg args: Any?) {
            Timber.v(throwable, message, *args)
        }
    }

    init {
        require(serverSettings.type == "smtp") { "Expected SMTP ServerSettings!" }
    }

    @Throws(MessagingException::class)
    override fun open() {
        try {
            var secureConnection = false
            val addresses = InetAddress.getAllByName(host)
            for ((index, address) in addresses.withIndex()) {
                try {
                    val socketAddress = InetSocketAddress(address, port)
                    if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
                        socket = trustedSocketFactory.createSocket(null, host, port, clientCertificateAlias).also {
                            it.connect(socketAddress, SOCKET_CONNECT_TIMEOUT)
                        }
                        secureConnection = true
                    } else {
                        socket = Socket().also {
                            it.connect(socketAddress, SOCKET_CONNECT_TIMEOUT)
                        }
                    }
                } catch (e: SocketException) {
                    if (index < addresses.lastIndex) {
                        // there are still other addresses for that host to try
                        continue
                    }

                    throw MessagingException("Cannot connect to host", e)
                }

                // connection success
                break
            }

            val socket = this.socket ?: error("socket == null")

            // RFC 1047
            socket.soTimeout = SOCKET_READ_TIMEOUT

            inputStream = PeekableInputStream(BufferedInputStream(socket.getInputStream(), 1024))
            responseParser = SmtpResponseParser(logger, inputStream!!)
            outputStream = BufferedOutputStream(socket.getOutputStream(), 1024)

            readGreeting()

            val helloName = buildHostnameToReport()
            var extensions = sendHello(helloName)

            is8bitEncodingAllowed = extensions.containsKey("8BITMIME")
            isEnhancedStatusCodesProvided = extensions.containsKey("ENHANCEDSTATUSCODES")
            isPipeliningSupported = extensions.containsKey("PIPELINING")

            if (connectionSecurity == ConnectionSecurity.STARTTLS_REQUIRED) {
                if (extensions.containsKey("STARTTLS")) {
                    executeCommand("STARTTLS")

                    val tlsSocket = trustedSocketFactory.createSocket(
                        socket,
                        host,
                        port,
                        clientCertificateAlias
                    )
                    this.socket = tlsSocket
                    inputStream = PeekableInputStream(BufferedInputStream(tlsSocket.getInputStream(), 1024))
                    responseParser = SmtpResponseParser(logger, inputStream!!)
                    outputStream = BufferedOutputStream(tlsSocket.getOutputStream(), 1024)

                    // Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically, Exim.
                    extensions = sendHello(helloName)
                    secureConnection = true
                } else {
                    // This exception triggers a "Certificate error" notification that takes the user to the incoming
                    // server settings for review. This might be needed if the account was configured with an obsolete
                    // "STARTTLS (if available)" setting.
                    throw CertificateValidationException("STARTTLS connection security not available")
                }
            }

            var authLoginSupported = false
            var authPlainSupported = false
            var authCramMD5Supported = false
            var authExternalSupported = false
            var authXoauth2Supported = false
            val saslMechanisms = extensions["AUTH"]
            if (saslMechanisms != null) {
                authLoginSupported = saslMechanisms.contains("LOGIN")
                authPlainSupported = saslMechanisms.contains("PLAIN")
                authCramMD5Supported = saslMechanisms.contains("CRAM-MD5")
                authExternalSupported = saslMechanisms.contains("EXTERNAL")
                authXoauth2Supported = saslMechanisms.contains("XOAUTH2")
            }
            parseOptionalSizeValue(extensions["SIZE"])

            if (
                username.isNotEmpty() &&
                (!password.isNullOrEmpty() || AuthType.EXTERNAL == authType || AuthType.XOAUTH2 == authType)
            ) {
                when (authType) {
                    AuthType.LOGIN, AuthType.PLAIN -> {
                        // try saslAuthPlain first, because it supports UTF-8 explicitly
                        if (authPlainSupported) {
                            saslAuthPlain()
                        } else if (authLoginSupported) {
                            saslAuthLogin()
                        } else {
                            throw MessagingException("Authentication methods SASL PLAIN and LOGIN are unavailable.")
                        }
                    }
                    AuthType.CRAM_MD5 -> {
                        if (authCramMD5Supported) {
                            saslAuthCramMD5()
                        } else {
                            throw MessagingException("Authentication method CRAM-MD5 is unavailable.")
                        }
                    }
                    AuthType.XOAUTH2 -> {
                        if (authXoauth2Supported && oauthTokenProvider != null) {
                            saslXoauth2()
                        } else {
                            throw MessagingException("Authentication method XOAUTH2 is unavailable.")
                        }
                    }
                    AuthType.EXTERNAL -> {
                        if (authExternalSupported) {
                            saslAuthExternal()
                        } else {
                            // Some SMTP servers are known to provide no error indication when a client certificate
                            // fails to validate, other than to not offer the AUTH EXTERNAL capability.
                            // So, we treat it is an error to not offer AUTH EXTERNAL when using client certificates.
                            // That way, the user can be notified of a problem during account setup.
                            throw CertificateValidationException(
                                CertificateValidationException.Reason.MissingCapability
                            )
                        }
                    }
                    AuthType.AUTOMATIC -> {
                        if (secureConnection) {
                            // try saslAuthPlain first, because it supports UTF-8 explicitly
                            if (authPlainSupported) {
                                saslAuthPlain()
                            } else if (authLoginSupported) {
                                saslAuthLogin()
                            } else if (authCramMD5Supported) {
                                saslAuthCramMD5()
                            } else {
                                throw MessagingException("No supported authentication methods available.")
                            }
                        } else {
                            if (authCramMD5Supported) {
                                saslAuthCramMD5()
                            } else {
                                // We refuse to insecurely transmit the password using the obsolete AUTOMATIC setting
                                // because of the potential for a MITM attack. Affected users must choose a different
                                // setting.
                                throw MessagingException(
                                    "Update your outgoing server authentication setting. " +
                                        "AUTOMATIC authentication is unavailable."
                                )
                            }
                        }
                    }
                    else -> {
                        throw MessagingException("Unhandled authentication method found in server settings (bug).")
                    }
                }
            }
        } catch (e: MessagingException) {
            close()
            throw e
        } catch (e: SSLException) {
            close()
            throw CertificateValidationException(e.message, e)
        } catch (e: GeneralSecurityException) {
            close()
            throw MessagingException("Unable to open connection to SMTP server due to security error.", e)
        } catch (e: IOException) {
            close()
            throw MessagingException("Unable to open connection to SMTP server.", e)
        }
    }

    private fun readGreeting() {
        val smtpResponse = responseParser!!.readGreeting()
        logResponse(smtpResponse)
    }

    private fun logResponse(smtpResponse: SmtpResponse, omitText: Boolean = false) {
        if (K9MailLib.isDebug()) {
            Timber.v("%s", smtpResponse.toLogString(omitText, linePrefix = "SMTP <<< "))
        }
    }

    private fun buildHostnameToReport(): String {
        val localAddress = socket!!.localAddress

        // We use local IP statically for privacy reasons, see https://github.com/k9mail/k-9/pull/3798
        return if (localAddress is Inet6Address) {
            "[IPv6:::1]"
        } else {
            "[127.0.0.1]"
        }
    }

    private fun parseOptionalSizeValue(sizeParameters: List<String>?) {
        if (sizeParameters != null && sizeParameters.isNotEmpty()) {
            val sizeParameter = sizeParameters.first()
            val size = sizeParameter.toIntOrNull()
            if (size != null) {
                largestAcceptableMessage = size
            } else {
                if (K9MailLib.isDebug() && K9MailLib.DEBUG_PROTOCOL_SMTP) {
                    Timber.d("SIZE parameter is not a valid integer: %s", sizeParameter)
                }
            }
        }
    }

    /**
     * Send the client "identity" using the `EHLO` or `HELO` command.
     *
     * We first try the EHLO command. If the server sends a negative response, it probably doesn't support the
     * `EHLO` command. So we try the older `HELO` command that all servers have to support. And if that fails, too,
     * we pretend everything is fine and continue unimpressed.
     *
     * @param host The EHLO/HELO parameter as defined by the RFC.
     *
     * @return A (possibly empty) `Map<String, List<String>>` of extensions (upper case) and their parameters
     * (possibly empty) as returned by the EHLO command.
     */
    private fun sendHello(host: String): Map<String, List<String>> {
        writeLine("EHLO $host")

        val helloResponse = responseParser!!.readHelloResponse()
        logResponse(helloResponse.response)

        return if (helloResponse is Hello) {
            helloResponse.keywords
        } else {
            if (K9MailLib.isDebug()) {
                Timber.v("Server doesn't support the EHLO command. Trying HELO...")
            }

            try {
                executeCommand("HELO %s", host)
            } catch (e: NegativeSmtpReplyException) {
                Timber.w("Server doesn't support the HELO command. Continuing anyway.")
            }

            emptyMap()
        }
    }

    @Throws(MessagingException::class)
    override fun sendMessage(message: Message) {
        val addresses = buildSet<String> {
            for (address in message.getRecipients(RecipientType.TO)) {
                add(address.address)
            }

            for (address in message.getRecipients(RecipientType.CC)) {
                add(address.address)
            }

            for (address in message.getRecipients(RecipientType.BCC)) {
                add(address.address)
            }
        }

        if (addresses.isEmpty()) {
            return
        }

        message.removeHeader("Bcc")

        close()
        open()

        // If the message has attachments and our server has told us about a limit on the size of messages, count
        // the message's size before sending it.
        if (largestAcceptableMessage > 0 && message.hasAttachments()) {
            if (message.calculateSize() > largestAcceptableMessage) {
                throw MessagingException("Message too large for server", true)
            }
        }

        var entireMessageSent = false
        try {
            val mailFrom = constructSmtpMailFromCommand(message.from, is8bitEncodingAllowed)
            if (isPipeliningSupported) {
                val pipelinedCommands = buildList {
                    add(mailFrom)

                    for (address in addresses) {
                        add(String.format("RCPT TO:<%s>", address))
                    }
                }

                executePipelinedCommands(pipelinedCommands)
                readPipelinedResponse(pipelinedCommands)
            } else {
                executeCommand(mailFrom)

                for (address in addresses) {
                    executeCommand("RCPT TO:<%s>", address)
                }
            }

            executeCommand("DATA")

            val msgOut = EOLConvertingOutputStream(
                LineWrapOutputStream(
                    SmtpDataStuffing(outputStream), 1000
                )
            )

            message.writeTo(msgOut)
            msgOut.endWithCrLfAndFlush()

            // After the "\r\n." is attempted, we may have sent the message
            entireMessageSent = true
            executeCommand(".")
        } catch (e: NegativeSmtpReplyException) {
            throw e
        } catch (e: Exception) {
            throw MessagingException("Unable to send message", entireMessageSent, e)
        } finally {
            close()
        }
    }

    private fun constructSmtpMailFromCommand(from: Array<Address>, is8bitEncodingAllowed: Boolean): String {
        val fromAddress = from.first().address
        return if (is8bitEncodingAllowed) {
            String.format("MAIL FROM:<%s> BODY=8BITMIME", fromAddress)
        } else {
            Timber.d("Server does not support 8-bit transfer encoding")
            String.format("MAIL FROM:<%s>", fromAddress)
        }
    }

    override fun close() {
        try {
            executeCommand("QUIT")
        } catch (ignored: Exception) {
        }

        IOUtils.closeQuietly(inputStream)
        IOUtils.closeQuietly(outputStream)
        IOUtils.closeQuietly(socket)

        inputStream = null
        responseParser = null
        outputStream = null
        socket = null
    }

    private fun writeLine(command: String, sensitive: Boolean = false) {
        if (K9MailLib.isDebug() && K9MailLib.DEBUG_PROTOCOL_SMTP) {
            val commandToLog = if (sensitive && !K9MailLib.isDebugSensitive()) {
                "SMTP >>> *sensitive*"
            } else {
                "SMTP >>> $command"
            }
            Timber.d(commandToLog)
        }

        // Important: Send command + CRLF using just one write() call. Using multiple calls might result in multiple
        // TCP packets being sent and some SMTP servers misbehave if CR and LF arrive in separate packets.
        // See https://code.google.com/archive/p/k9mail/issues/799
        val data = (command + "\r\n").toByteArray()
        outputStream!!.apply {
            write(data)
            flush()
        }
    }

    private fun executeSensitiveCommand(format: String, vararg args: Any): SmtpResponse {
        return executeCommand(sensitive = true, format, *args)
    }

    private fun executeCommand(format: String, vararg args: Any): SmtpResponse {
        return executeCommand(sensitive = false, format, *args)
    }

    private fun executeCommand(sensitive: Boolean, format: String, vararg args: Any): SmtpResponse {
        val command = String.format(Locale.ROOT, format, *args)
        writeLine(command, sensitive)

        val response = responseParser!!.readResponse(isEnhancedStatusCodesProvided)
        logResponse(response, sensitive)

        if (response.isNegativeResponse) {
            throw buildNegativeSmtpReplyException(response)
        }

        return response
    }

    private fun buildNegativeSmtpReplyException(response: SmtpResponse): NegativeSmtpReplyException {
        return NegativeSmtpReplyException(
            replyCode = response.replyCode,
            replyText = response.joinedText,
            enhancedStatusCode = response.enhancedStatusCode
        )
    }

    private fun executePipelinedCommands(pipelinedCommands: List<String>) {
        for (command in pipelinedCommands) {
            writeLine(command, false)
        }
    }

    private fun readPipelinedResponse(pipelinedCommands: List<String>) {
        val responseParser = responseParser!!
        var firstException: MessagingException? = null

        repeat(pipelinedCommands.size) {
            val response = responseParser.readResponse(isEnhancedStatusCodesProvided)
            logResponse(response, omitText = false)

            if (response.isNegativeResponse && firstException == null) {
                firstException = buildNegativeSmtpReplyException(response)
            }
        }

        firstException?.let {
            throw it
        }
    }

    private fun saslAuthLogin() {
        try {
            executeCommand("AUTH LOGIN")
            executeSensitiveCommand(Base64.encode(username))
            executeSensitiveCommand(Base64.encode(password))
        } catch (exception: NegativeSmtpReplyException) {
            if (exception.replyCode == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw AuthenticationFailedException("AUTH LOGIN failed (${exception.message})")
            } else {
                throw exception
            }
        }
    }

    private fun saslAuthPlain() {
        val data = Base64.encode("\u0000" + username + "\u0000" + password)
        try {
            executeSensitiveCommand("AUTH PLAIN %s", data)
        } catch (exception: NegativeSmtpReplyException) {
            if (exception.replyCode == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw AuthenticationFailedException("AUTH PLAIN failed (${exception.message})")
            } else {
                throw exception
            }
        }
    }

    private fun saslAuthCramMD5() {
        val respList = executeCommand("AUTH CRAM-MD5").texts
        if (respList.size != 1) {
            throw MessagingException("Unable to negotiate CRAM-MD5")
        }

        val b64Nonce = respList[0]
        val b64CRAMString = Authentication.computeCramMd5(username, password, b64Nonce)
        try {
            executeSensitiveCommand(b64CRAMString)
        } catch (exception: NegativeSmtpReplyException) {
            if (exception.replyCode == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw AuthenticationFailedException(exception.message!!, exception)
            } else {
                throw exception
            }
        }
    }

    private fun saslXoauth2() {
        retryXoauthWithNewToken = true
        try {
            attemptXoauth2(username)
        } catch (negativeResponse: NegativeSmtpReplyException) {
            if (negativeResponse.replyCode != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponse
            }

            oauthTokenProvider!!.invalidateToken()

            if (!retryXoauthWithNewToken) {
                handlePermanentFailure(negativeResponse)
            } else {
                handleTemporaryFailure(username, negativeResponse)
            }
        }
    }

    private fun handlePermanentFailure(negativeResponse: NegativeSmtpReplyException): Nothing {
        throw AuthenticationFailedException(negativeResponse.message!!, negativeResponse)
    }

    private fun handleTemporaryFailure(username: String, negativeResponseFromOldToken: NegativeSmtpReplyException) {
        // Token was invalid. We could avoid this double check if we had a reasonable chance of knowing if a token was
        // invalid before use (e.g. due to expiry). But we don't. This is the intended behaviour per AccountManager.
        Timber.v(negativeResponseFromOldToken, "Authentication exception, re-trying with new token")

        try {
            attemptXoauth2(username)
        } catch (negativeResponseFromNewToken: NegativeSmtpReplyException) {
            if (negativeResponseFromNewToken.replyCode != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponseFromNewToken
            }

            // Okay, we failed on a new token. Invalidate the token anyway but assume it's permanent.
            Timber.v(negativeResponseFromNewToken, "Authentication exception for new token, permanent error assumed")

            oauthTokenProvider!!.invalidateToken()
            handlePermanentFailure(negativeResponseFromNewToken)
        }
    }

    private fun attemptXoauth2(username: String) {
        val token = oauthTokenProvider!!.getToken(OAuth2TokenProvider.OAUTH2_TIMEOUT.toLong())
        val authString = Authentication.computeXoauth(username, token)

        val response = executeSensitiveCommand("AUTH XOAUTH2 %s", authString)
        if (response.replyCode == SMTP_CONTINUE_REQUEST) {
            val replyText = response.joinedText
            retryXoauthWithNewToken = XOAuth2ChallengeParser.shouldRetry(replyText, host)

            // Per Google spec, respond to challenge with empty response
            executeCommand("")
        }
    }

    private fun saslAuthExternal() {
        executeCommand("AUTH EXTERNAL %s", Base64.encode(username))
    }

    @Throws(MessagingException::class)
    fun checkSettings() {
        close()

        try {
            open()
        } finally {
            close()
        }
    }
}
