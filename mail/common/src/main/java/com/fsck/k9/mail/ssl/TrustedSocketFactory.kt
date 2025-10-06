package com.fsck.k9.mail.ssl

import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import net.thunderbird.core.common.exception.MessagingException

interface TrustedSocketFactory {
    @Throws(
        NoSuchAlgorithmException::class,
        KeyManagementException::class,
        MessagingException::class,
        IOException::class,
    )
    fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket
}
