package app.k9mail.autodiscovery.demo

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.mail.toDomain
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings

class DemoAutoDiscovery : AutoDiscovery {
    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        val domain = email.domain.toDomain()

        return listOf(
            AutoDiscoveryRunnable {
                if (domain.value == "example.com") {
                    AutoDiscoveryResult.Settings(
                        incomingServerSettings = DemoServerSettings,
                        outgoingServerSettings = DemoServerSettings,
                        isTrusted = true,
                        source = "DemoAutoDiscovery",
                    )
                } else {
                    AutoDiscoveryResult.NoUsableSettingsFound
                }
            },
        )
    }
}

object DemoServerSettings : IncomingServerSettings, OutgoingServerSettings {
    val serverSettings = ServerSettings(
        type = "demo",
        host = "irrelevant",
        port = 23,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = AuthType.AUTOMATIC,
        username = "irrelevant",
        password = "irrelevant",
        clientCertificateAlias = null,
    )
}
