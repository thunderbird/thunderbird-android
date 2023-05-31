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
import java.io.InputStream

internal class MockAutoconfigParser : AutoconfigParser {
    val callArguments = mutableListOf<Pair<String, EmailAddress>>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<AutoDiscoveryResult?>()

    fun addResult(discoveryResult: AutoDiscoveryResult?) {
        results.add(discoveryResult)
    }

    override fun parseSettings(inputStream: InputStream, email: EmailAddress): AutoDiscoveryResult? {
        val data = String(inputStream.readBytes())
        callArguments.add(data to email)

        check(results.isNotEmpty()) { "parseSettings($data, $email) called but no result provided" }
        return results.removeAt(0)
    }

    companion object {
        val RESULT_ONE = AutoDiscoveryResult(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.domain.example".toHostname(),
                port = 993.toPort(),
                connectionSecurity = TLS,
                authenticationType = PasswordCleartext,
                username = "irrelevant@domain.example",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.domain.example".toHostname(),
                port = 465.toPort(),
                connectionSecurity = TLS,
                authenticationType = PasswordCleartext,
                username = "irrelevant@domain.example",
            ),
        )
        val RESULT_TWO = AutoDiscoveryResult(
            incomingServerSettings = ImapServerSettings(
                hostname = "imap.company.example".toHostname(),
                port = 143.toPort(),
                connectionSecurity = StartTLS,
                authenticationType = PasswordEncrypted,
                username = "irrelevant@company.example",
            ),
            outgoingServerSettings = SmtpServerSettings(
                hostname = "smtp.company.example".toHostname(),
                port = 587.toPort(),
                connectionSecurity = StartTLS,
                authenticationType = PasswordEncrypted,
                username = "irrelevant@company.example",
            ),
        )
    }
}
