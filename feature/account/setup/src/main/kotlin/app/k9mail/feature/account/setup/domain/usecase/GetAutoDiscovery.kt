package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.feature.account.setup.domain.DomainContract
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.delay

internal class GetAutoDiscovery(
    private val service: AutoDiscoveryService,
) : DomainContract.UseCase.GetAutoDiscovery {
    override suspend fun execute(emailAddress: String): AutoDiscoveryResult {
        val fakeResult: AutoDiscoveryResult? = if (emailAddress.contains("empty")) {
            AutoDiscoveryResult.NoUsableSettingsFound
        } else if (emailAddress.contains("test")) {
            getFakeAutoDiscovery(emailAddress)
        } else if (emailAddress.contains("error")) {
            AutoDiscoveryResult.NetworkError(IOException("Failed to load config"))
        } else if (emailAddress.contains("unexpected")) {
            AutoDiscoveryResult.UnexpectedException(Exception("Unexpected exception"))
        } else {
            null
        }

        if (fakeResult != null) {
            return provideWithDelay(fakeResult)
        }

        return service.discover(emailAddress.toUserEmailAddress())
    }

    @Suppress("MagicNumber")
    private suspend fun provideWithDelay(autoDiscoveryResult: AutoDiscoveryResult): AutoDiscoveryResult {
        delay(Random(0).nextLong(500, 2000))
        return autoDiscoveryResult
    }

    @Suppress("MagicNumber")
    private fun getFakeAutoDiscovery(emailAddress: String): AutoDiscoveryResult.Settings {
        val hasIncomingOauth = emailAddress.contains("in")
        val hasOutgoingOauth = emailAddress.contains("out")
        val isTrusted = emailAddress.contains("trust")

        return AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.${getHost(emailAddress)}".toHostname(),
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
                hostname = "smtp.${getHost(emailAddress)}".toHostname(),
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
        )
    }

    private fun getHost(emailAddress: String) = emailAddress.split("@").last()
}
