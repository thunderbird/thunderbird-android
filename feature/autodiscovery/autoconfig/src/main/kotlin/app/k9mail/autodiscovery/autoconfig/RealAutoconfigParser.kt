package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AuthenticationType.OAuth2
import app.k9mail.autodiscovery.api.AuthenticationType.PasswordCleartext
import app.k9mail.autodiscovery.api.AuthenticationType.PasswordEncrypted
import app.k9mail.autodiscovery.api.ConnectionSecurity
import app.k9mail.autodiscovery.api.ConnectionSecurity.StartTLS
import app.k9mail.autodiscovery.api.ConnectionSecurity.TLS
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import java.io.InputStream
import java.io.InputStreamReader
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.net.HostNameUtils
import net.thunderbird.core.common.net.Hostname
import net.thunderbird.core.common.net.Port
import net.thunderbird.core.common.net.toHostname
import net.thunderbird.core.common.net.toPort
import net.thunderbird.core.logging.legacy.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

private typealias ServerSettingsFactory<T> = (
    hostname: Hostname,
    port: Port,
    connectionSecurity: ConnectionSecurity,
    authenticationTypes: List<AuthenticationType>,
    username: String,
) -> T

internal class RealAutoconfigParser : AutoconfigParser {
    override fun parseSettings(inputStream: InputStream, email: EmailAddress): AutoconfigParserResult {
        return try {
            ClientConfigParser(inputStream, email).parse()
        } catch (e: XmlPullParserException) {
            AutoconfigParserResult.ParserError(error = AutoconfigParserException("Error parsing Autoconfig XML", e))
        } catch (e: AutoconfigParserException) {
            AutoconfigParserResult.ParserError(e)
        }
    }
}

@Suppress("TooManyFunctions")
private class ClientConfigParser(
    private val inputStream: InputStream,
    private val email: EmailAddress,
) {
    private val localPart = email.localPart
    private val domain = email.domain.normalized

    private val pullParser: XmlPullParser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(InputStreamReader(inputStream))
    }

    fun parse(): AutoconfigParserResult {
        var result: AutoconfigParserResult? = null
        do {
            val eventType = pullParser.next()

            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "clientConfig" -> {
                        result = parseClientConfig()
                    }
                    else -> skipElement()
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT)

        if (result == null) {
            parserError("Missing 'clientConfig' element")
        }

        return result
    }

    private fun parseClientConfig(): AutoconfigParserResult {
        var result: AutoconfigParserResult? = null

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "emailProvider" -> {
                        result = parseEmailProvider()
                    }
                    else -> skipElement()
                }
            }
        }

        return result ?: parserError("Missing 'emailProvider' element")
    }

    private fun parseEmailProvider(): AutoconfigParserResult {
        var domainFound = false
        val incomingServerSettings = mutableListOf<IncomingServerSettings>()
        val outgoingServerSettings = mutableListOf<OutgoingServerSettings>()

        // The 'id' attribute is required (but not really used) by Thunderbird desktop.
        val emailProviderId = pullParser.getAttributeValue(null, "id")
        if (emailProviderId == null) {
            parserError("Missing 'emailProvider.id' attribute")
        } else if (!emailProviderId.isValidHostname()) {
            parserError("Invalid 'emailProvider.id' attribute")
        }

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "domain" -> {
                        val domain = readText().replaceVariables()
                        if (domain.isValidHostname()) {
                            domainFound = true
                        }
                    }
                    "incomingServer" -> {
                        parseServer("imap", ::createImapServerSettings)?.let { serverSettings ->
                            incomingServerSettings.add(serverSettings)
                        }
                    }
                    "outgoingServer" -> {
                        parseServer("smtp", ::createSmtpServerSettings)?.let { serverSettings ->
                            outgoingServerSettings.add(serverSettings)
                        }
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        // Thunderbird desktop requires at least one valid 'domain' element.
        if (!domainFound) {
            parserError("Valid 'domain' element required")
        }

        if (incomingServerSettings.isEmpty()) {
            parserError("No supported 'incomingServer' element found")
        }

        if (outgoingServerSettings.isEmpty()) {
            parserError("No supported 'outgoingServer' element found")
        }

        return AutoconfigParserResult.Settings(
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
        )
    }

    private fun <T> parseServer(
        protocolType: String,
        createServerSettings: ServerSettingsFactory<T>,
    ): T? {
        val type = pullParser.getAttributeValue(null, "type")
        if (type != protocolType) {
            Log.d("Unsupported '%s[type]' value: '%s'", pullParser.name, type)
            skipElement()
            return null
        }

        var hostname: String? = null
        var port: Int? = null
        var userName: String? = null
        val authenticationTypes = mutableListOf<AuthenticationType>()
        var connectionSecurity: ConnectionSecurity? = null

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "hostname" -> hostname = readHostname()
                    "port" -> port = readPort()
                    "username" -> userName = readUsername()
                    "authentication" -> readAuthentication(authenticationTypes)
                    "socketType" -> connectionSecurity = readSocketType()
                }
            }
        }

        val finalHostname = hostname ?: parserError("Missing 'hostname' element")
        val finalPort = port ?: parserError("Missing 'port' element")
        val finalUserName = userName ?: parserError("Missing 'username' element")
        val finalAuthenticationTypes = if (authenticationTypes.isNotEmpty()) {
            authenticationTypes.toList()
        } else {
            parserError("No usable 'authentication' element found")
        }
        val finalConnectionSecurity = connectionSecurity ?: parserError("Missing 'socketType' element")

        return createServerSettings(
            finalHostname.toHostname(),
            finalPort.toPort(),
            finalConnectionSecurity,
            finalAuthenticationTypes,
            finalUserName,
        )
    }

    private fun readHostname(): String {
        val hostNameText = readText()
        val hostName = hostNameText.replaceVariables()
        return hostName.takeIf { it.isValidHostname() }
            ?: parserError("Invalid 'hostname' value: '$hostNameText'")
    }

    private fun readPort(): Int {
        val portText = readText()
        return portText.toIntOrNull()?.takeIf { it.isValidPort() }
            ?: parserError("Invalid 'port' value: '$portText'")
    }

    private fun readUsername(): String = readText().replaceVariables()

    private fun readAuthentication(authenticationTypes: MutableList<AuthenticationType>) {
        val authenticationType = readText().toAuthenticationType() ?: return
        authenticationTypes.add(authenticationType)
    }

    private fun readSocketType() = readText().toConnectionSecurity()

    private fun String.toAuthenticationType(): AuthenticationType? {
        return when (this) {
            "OAuth2" -> OAuth2
            "password-cleartext" -> PasswordCleartext
            "password-encrypted" -> PasswordEncrypted
            else -> {
                Log.d("Ignoring unknown 'authentication' value '$this'")
                null
            }
        }
    }

    private fun String.toConnectionSecurity(): ConnectionSecurity {
        return when (this) {
            "SSL" -> TLS
            "STARTTLS" -> StartTLS
            else -> parserError("Unknown 'socketType' value: '$this'")
        }
    }

    private fun readElement(block: (Int) -> Unit) {
        require(pullParser.eventType == XmlPullParser.START_TAG)

        val tagName = pullParser.name
        val depth = pullParser.depth
        while (true) {
            when (val eventType = pullParser.next()) {
                XmlPullParser.END_DOCUMENT -> {
                    parserError("End of document reached while reading element '$tagName'")
                }
                XmlPullParser.END_TAG -> {
                    if (pullParser.name == tagName && pullParser.depth == depth) return
                }
                else -> {
                    block(eventType)
                }
            }
        }
    }

    private fun readText(): String {
        var text = ""
        readElement { eventType ->
            when (eventType) {
                XmlPullParser.TEXT -> {
                    text = pullParser.text
                }
                else -> {
                    parserError("Expected text, but got ${XmlPullParser.TYPES[eventType]}")
                }
            }
        }

        return text
    }

    private fun skipElement() {
        Log.d("Skipping element '%s'", pullParser.name)
        readElement { /* Do nothing */ }
    }

    private fun parserError(message: String): Nothing {
        throw AutoconfigParserException(message)
    }

    private fun String.isValidHostname(): Boolean {
        val cleanUpHostName = HostNameUtils.cleanUpHostName(this)
        return HostNameUtils.isLegalHostNameOrIP(cleanUpHostName) != null
    }

    @Suppress("MagicNumber")
    private fun Int.isValidPort() = this in 0..65535

    private fun String.replaceVariables(): String {
        return replace("%EMAILDOMAIN%", domain)
            .replace("%EMAILLOCALPART%", localPart)
            .replace("%EMAILADDRESS%", email.address)
    }

    private fun createImapServerSettings(
        hostname: Hostname,
        port: Port,
        connectionSecurity: ConnectionSecurity,
        authenticationTypes: List<AuthenticationType>,
        username: String,
    ): ImapServerSettings {
        return ImapServerSettings(hostname, port, connectionSecurity, authenticationTypes, username)
    }

    private fun createSmtpServerSettings(
        hostname: Hostname,
        port: Port,
        connectionSecurity: ConnectionSecurity,
        authenticationTypes: List<AuthenticationType>,
        username: String,
    ): SmtpServerSettings {
        return SmtpServerSettings(hostname, port, connectionSecurity, authenticationTypes, username)
    }
}
