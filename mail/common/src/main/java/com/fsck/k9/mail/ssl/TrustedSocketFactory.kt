package com.fsck.k9.mail.ssl

import com.fsck.k9.mail.MessagingException
import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

interface TrustedSocketFactory {
    @Throws(
        NoSuchAlgorithmException::class,
        KeyManagementException::class,
        MessagingException::class,
        IOException::class,
    )
    fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket
}
