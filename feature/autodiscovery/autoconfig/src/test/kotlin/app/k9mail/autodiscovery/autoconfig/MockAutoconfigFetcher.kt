package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AuthenticationType.PasswordCleartext
import app.k9mail.autodiscovery.api.AuthenticationType.PasswordEncrypted
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ConnectionSecurity.StartTLS
import app.k9mail.autodiscovery.api.ConnectionSecurity.TLS
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import okhttp3.HttpUrl

internal class MockAutoconfigFetcher : AutoconfigFetcher {
    val callArguments = mutableListOf<Pair<HttpUrl, EmailAddress>>()

    val callCount: Int
        get() = callArguments.size

    val urls: List<String>
        get() = callArguments.map { (url, _) -> url.toString() }

    private val results = mutableListOf<AutoDiscoveryResult>()

    fun addResult(discoveryResult: AutoDiscoveryResult) {
        results.add(discoveryResult)
    }

    override suspend fun fetchAutoconfig(autoconfigUrl: HttpUrl, email: EmailAddress): AutoDiscoveryResult {
        callArguments.add(autoconfigUrl to email)

        check(results.isNotEmpty()) {
            "MockAutoconfigFetcher.fetchAutoconfig($autoconfigUrl) called but no result provided"
        }
        return results.removeAt(0)
    }

    companion object {
        val RESULT_ONE = AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.domain.example".toHostname(),
                port = 993.toPort(),
                connectionSecurity = TLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "irrelevant@domain.example",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.domain.example".toHostname(),
                port = 465.toPort(),
                connectionSecurity = TLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "irrelevant@domain.example",
            ),
            isTrusted = true,
            source = "result 1",
        )
        val RESULT_TWO = AutoDiscoveryResult.Settings(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.company.example".toHostname(),
                port = 143.toPort(),
                connectionSecurity = StartTLS,
                authenticationTypes = listOf(PasswordEncrypted),
                username = "irrelevant@company.example",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.company.example".toHostname(),
                port = 587.toPort(),
                connectionSecurity = StartTLS,
                authenticationTypes = listOf(PasswordEncrypted),
                username = "irrelevant@company.example",
            ),
            isTrusted = true,
            source = "result 2",
        )
    }
}
