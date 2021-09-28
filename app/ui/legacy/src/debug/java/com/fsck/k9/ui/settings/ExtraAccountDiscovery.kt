package com.fsck.k9.ui.settings

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.ui.ConnectionSettings

object ExtraAccountDiscovery {
    @JvmStatic
    fun discover(email: String): ConnectionSettings? {
        return if (email.endsWith("@k9mail.example")) {
            val serverSettings = ServerSettings(
                type = "demo",
                host = "irrelevant",
                port = 23,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.AUTOMATIC,
                username = "irrelevant",
                password = "irrelevant",
                clientCertificateAlias = null
            )
            ConnectionSettings(incoming = serverSettings, outgoing = serverSettings)
        } else {
            null
        }
    }
}
