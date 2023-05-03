package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.DiscoveredServerSettings
import app.k9mail.autodiscovery.api.DiscoveryResults
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.helper.HostNameUtils
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import java.io.InputStream
import java.io.InputStreamReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser for Thunderbird's Autoconfig file format.
 *
 * See [https://github.com/thundernest/autoconfig](https://github.com/thundernest/autoconfig)
 */
class AutoconfigParser {
    fun parseSettings(stream: InputStream, email: String): DiscoveryResults {
        return try {
            ClientConfigParser(stream, email).parse()
        } catch (e: XmlPullParserException) {
            throw AutoconfigParserException("Error parsing Autoconfig XML", e)
        }
    }
}

@Suppress("TooManyFunctions")
private class ClientConfigParser(
    private val inputStream: InputStream,
    private val email: String,
) {
    private val localPart = requireNotNull(EmailHelper.getLocalPartFromEmailAddress(email)) { "Invalid email address" }
    private val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email)) { "Invalid email address" }

    private val pullParser: XmlPullParser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(InputStreamReader(inputStream))
    }

    private val incomingServers = mutableListOf<DiscoveredServerSettings>()
    private val outgoingServers = mutableListOf<DiscoveredServerSettings>()

    fun parse(): DiscoveryResults {
        var clientConfigFound = false
        do {
            val eventType = pullParser.next()

            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "clientConfig" -> {
                        clientConfigFound = true
                        parseClientConfig()
                    }
                    else -> skipElement()
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT)

        if (!clientConfigFound) {
            parserError("Missing 'clientConfig' element")
        }

        return DiscoveryResults(incomingServers, outgoingServers)
    }

    private fun parseClientConfig() {
        var emailProviderFound = false

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "emailProvider" -> {
                        emailProviderFound = true
                        parseEmailProvider()
                    }
                    else -> skipElement()
                }
            }
        }

        if (!emailProviderFound) {
            parserError("Missing 'emailProvider' element")
        }
    }

    private fun parseEmailProvider() {
        var domainFound = false

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
                        val domain = readText()
                        if (domain.isValidHostname()) {
                            domainFound = true
                        }
                    }
                    "incomingServer" -> {
                        parseServer()?.let { serverSettings ->
                            incomingServers.add(serverSettings)
                        }
                    }
                    "outgoingServer" -> {
                        parseServer()?.let { serverSe ->
                            outgoingServers.add(serverSe)
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

        if (incomingServers.isEmpty()) {
            parserError("Missing 'incomingServer' element")
        }

        if (outgoingServers.isEmpty()) {
            parserError("Missing 'outgoingServer' element")
        }
    }

    private fun parseServer(): DiscoveredServerSettings? {
        val type = pullParser.getAttributeValue(null, "type")
        if (type != "imap" && type != "smtp") {
            return null
        }

        var hostName: String? = null
        var port: Int? = null
        var userName: String? = null
        var authType: AuthType? = null
        var connectionSecurity: ConnectionSecurity? = null

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    "hostname" -> hostName = readHostname()
                    "port" -> port = readPort()
                    "username" -> userName = readUsername()
                    "authentication" -> authType = readAuthentication(authType)
                    "socketType" -> connectionSecurity = readSocketType()
                }
            }
        }

        if (hostName == null) {
            parserError("Missing 'hostname' element")
        } else if (port == null) {
            parserError("Missing 'port' element")
        } else if (userName == null) {
            parserError("Missing 'username' element")
        } else if (authType == null) {
            parserError("No usable 'authentication' element found")
        } else if (connectionSecurity == null) {
            parserError("Missing 'socketType' element")
        }

        return DiscoveredServerSettings(type, hostName!!, port!!, connectionSecurity!!, authType, userName)
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

    private fun readAuthentication(authType: AuthType?): AuthType? {
        return authType ?: readText().toAuthType()
    }

    private fun readSocketType() = readText().toConnectionSecurity()

    private fun String.toAuthType(): AuthType? {
        return when (this) {
            "OAuth2" -> AuthType.XOAUTH2
            "password-cleartext" -> AuthType.PLAIN
            "password-encrypted" -> AuthType.CRAM_MD5
            else -> {
                Timber.d("Ignoring unknown 'authentication' value '$this'")
                null
            }
        }
    }

    private fun String.toConnectionSecurity(): ConnectionSecurity {
        return when (this) {
            "SSL" -> ConnectionSecurity.SSL_TLS_REQUIRED
            "STARTTLS" -> ConnectionSecurity.STARTTLS_REQUIRED
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
        Timber.d("Skipping element '%s'", pullParser.name)
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
            .replace("%EMAILADDRESS%", email)
    }
}

class AutoconfigParserException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
