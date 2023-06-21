package app.k9mail.feature.account.setup.data

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import java.io.IOException
import java.lang.UnsupportedOperationException
import kotlin.random.Random
import kotlinx.coroutines.delay

class FakeAutoDiscoveryService : AutoDiscoveryService {
    override suspend fun discover(email: EmailAddress): AutoDiscoveryResult {
        val result: AutoDiscoveryResult? = handleFakeResponse(email)
        return if (result != null) {
            provideWithDelay(result)
        } else {
            AutoDiscoveryResult.UnexpectedException(
                UnsupportedOperationException("No fake response for $email"),
            )
        }
    }

    @Suppress("MagicNumber")
    private suspend fun provideWithDelay(autoDiscoveryResult: AutoDiscoveryResult): AutoDiscoveryResult {
        delay(Random(0).nextLong(500, 2000))
        return autoDiscoveryResult
    }

    private fun handleFakeResponse(emailAddress: EmailAddress): AutoDiscoveryResult? {
        return if (emailAddress.localPart.contains("empty")) {
            AutoDiscoveryResult.NoUsableSettingsFound
        } else if (emailAddress.localPart.contains("test")) {
            getFakeAutoDiscovery(emailAddress)
        } else if (emailAddress.localPart.contains("error")) {
            AutoDiscoveryResult.NetworkError(IOException("Failed to load config"))
        } else if (emailAddress.localPart.contains("unexpected")) {
            AutoDiscoveryResult.UnexpectedException(Exception("Unexpected exception"))
        } else {
            null
        }
    }

    @Suppress("MagicNumber")
    private fun getFakeAutoDiscovery(emailAddress: EmailAddress): AutoDiscoveryResult.Settings {
        val hasIncomingOauth = emailAddress.localPart.contains("in")
        val hasOutgoingOauth = emailAddress.localPart.contains("out")
        val isTrusted = emailAddress.localPart.contains("trust")

        return AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.${emailAddress.domain}".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationType = if (hasIncomingOauth) {
                    AuthenticationType.OAuth2
                } else {
                    AuthenticationType.PasswordEncrypted
                },
                username = "username",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.${emailAddress.domain}".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.TLS,
                authenticationType = if (hasOutgoingOauth) {
                    AuthenticationType.OAuth2
                } else {
                    AuthenticationType.PasswordEncrypted
                },
                username = "username",
            ),
            isTrusted = isTrusted,
            source = "fake",
        )
    }
}
