package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AuthenticationType.OAuth2
import app.k9mail.autodiscovery.api.AuthenticationType.PasswordCleartext
import app.k9mail.autodiscovery.api.ConnectionSecurity.StartTLS
import app.k9mail.autodiscovery.api.ConnectionSecurity.TLS
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.autodiscovery.autoconfig.AutoconfigParserResult.ParserError
import app.k9mail.autodiscovery.autoconfig.AutoconfigParserResult.Settings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import java.io.InputStream
import net.thunderbird.core.common.mail.toUserEmailAddress
import net.thunderbird.core.common.net.toHostname
import net.thunderbird.core.common.net.toPort
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.junit.Test

private const val PRINT_MODIFIED_XML = false

class RealAutoconfigParserTest {
    private val parser = RealAutoconfigParser()

    @Language("XML")
    private val minimalConfig =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <clientConfig version="1.1">
          <emailProvider id="domain.example">
            <domain>domain.example</domain>
            <incomingServer type="imap">
              <hostname>imap.domain.example</hostname>
              <port>993</port>
              <socketType>SSL</socketType>
              <authentication>password-cleartext</authentication>
              <username>%EMAILADDRESS%</username>
            </incomingServer>
            <outgoingServer type="smtp">
              <hostname>smtp.domain.example</hostname>
              <port>587</port>
              <socketType>STARTTLS</socketType>
              <authentication>password-cleartext</authentication>
              <username>%EMAILADDRESS%</username>
            </outgoingServer>
          </emailProvider>
        </clientConfig>
        """.trimIndent()

    @Language("XML")
    private val additionalIncomingServer =
        """
        <incomingServer type="imap">
          <hostname>imap.domain.example</hostname>
          <port>143</port>
          <socketType>STARTTLS</socketType>
          <authentication>password-cleartext</authentication>
          <username>%EMAILADDRESS%</username>
        </incomingServer>
        """.trimIndent()

    @Language("XML")
    private val additionalOutgoingServer =
        """
        <outgoingServer type="smtp">
          <hostname>smtp.domain.example</hostname>
          <port>465</port>
          <socketType>SSL</socketType>
          <authentication>password-cleartext</authentication>
          <username>%EMAILADDRESS%</username>
        </outgoingServer>
        """.trimIndent()

    private val irrelevantEmailAddress = "irrelevant@domain.example".toUserEmailAddress()

    @Test
    fun `minimal data`() {
        val inputStream = minimalConfig.byteInputStream()

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isNotNull().isEqualTo(
            Settings(
                incomingServerSettings = listOf(
                    ImapServerSettings(
                        hostname = "imap.domain.example".toHostname(),
                        port = 993.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                ),
                outgoingServerSettings = listOf(
                    SmtpServerSettings(
                        hostname = "smtp.domain.example".toHostname(),
                        port = 587.toPort(),
                        connectionSecurity = StartTLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `real-world data`() {
        val inputStream = javaClass.getResourceAsStream("/2022-11-19-googlemail.com.xml")!!

        val result = parser.parseSettings(inputStream, email = "test@gmail.com".toUserEmailAddress())

        assertThat(result).isNotNull().isEqualTo(
            Settings(
                incomingServerSettings = listOf(
                    ImapServerSettings(
                        hostname = "imap.gmail.com".toHostname(),
                        port = 993.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(OAuth2, PasswordCleartext),
                        username = "test@gmail.com",
                    ),
                ),
                outgoingServerSettings = listOf(
                    SmtpServerSettings(
                        hostname = "smtp.gmail.com".toHostname(),
                        port = 465.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(OAuth2, PasswordCleartext),
                        username = "test@gmail.com",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `multiple incomingServer and outgoingServer elements`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer").insertBefore(additionalIncomingServer)
            element("outgoingServer").insertBefore(additionalOutgoingServer)
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isNotNull().isEqualTo(
            Settings(
                incomingServerSettings = listOf(
                    ImapServerSettings(
                        hostname = "imap.domain.example".toHostname(),
                        port = 143.toPort(),
                        connectionSecurity = StartTLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                    ImapServerSettings(
                        hostname = "imap.domain.example".toHostname(),
                        port = 993.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                ),
                outgoingServerSettings = listOf(
                    SmtpServerSettings(
                        hostname = "smtp.domain.example".toHostname(),
                        port = 465.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                    SmtpServerSettings(
                        hostname = "smtp.domain.example".toHostname(),
                        port = 587.toPort(),
                        connectionSecurity = StartTLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `replace variables`() {
        val inputStream = minimalConfig.withModifications {
            element("domain").text("%EMAILDOMAIN%")
            element("incomingServer > hostname").text("%EMAILLOCALPART%.domain.example")
            element("outgoingServer > hostname").text("%EMAILLOCALPART%.outgoing.domain.example")
            element("outgoingServer > username").text("%EMAILDOMAIN%")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isNotNull().isEqualTo(
            Settings(
                incomingServerSettings = listOf(
                    ImapServerSettings(
                        hostname = "user.domain.example".toHostname(),
                        port = 993.toPort(),
                        connectionSecurity = TLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "user@domain.example",
                    ),
                ),
                outgoingServerSettings = listOf(
                    SmtpServerSettings(
                        hostname = "user.outgoing.domain.example".toHostname(),
                        port = 587.toPort(),
                        connectionSecurity = StartTLS,
                        authenticationTypes = listOf(PasswordCleartext),
                        username = "domain.example",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `data with comments`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").prepend("<!-- comment -->")
            element("incomingServer > port").prepend("<!-- comment -->")
            element("incomingServer > socketType").prepend("<!-- comment -->")
            element("incomingServer > authentication").prepend("<!-- comment -->")
            element("incomingServer > username").prepend("<!-- comment -->")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isInstanceOf<Settings>()
            .prop(Settings::incomingServerSettings).containsExactly(
                ImapServerSettings(
                    hostname = "imap.domain.example".toHostname(),
                    port = 993.toPort(),
                    connectionSecurity = TLS,
                    authenticationTypes = listOf(PasswordCleartext),
                    username = "user@domain.example",
                ),
            )
    }

    @Test
    fun `ignore unsupported 'incomingServer' type`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer").insertBefore("""<incomingServer type="smtp"/>""")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isInstanceOf<Settings>()
            .prop(Settings::incomingServerSettings).containsExactly(
                ImapServerSettings(
                    hostname = "imap.domain.example".toHostname(),
                    port = 993.toPort(),
                    connectionSecurity = TLS,
                    authenticationTypes = listOf(PasswordCleartext),
                    username = "user@domain.example",
                ),
            )
    }

    @Test
    fun `ignore unsupported 'outgoingServer' type`() {
        val inputStream = minimalConfig.withModifications {
            element("outgoingServer").insertBefore("""<outgoingServer type="imap"/>""")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isInstanceOf<Settings>()
            .prop(Settings::outgoingServerSettings).containsExactly(
                SmtpServerSettings(
                    hostname = "smtp.domain.example".toHostname(),
                    port = 587.toPort(),
                    connectionSecurity = StartTLS,
                    authenticationTypes = listOf(PasswordCleartext),
                    username = "user@domain.example",
                ),
            )
    }

    @Test
    fun `empty authentication element should be ignored`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > authentication").insertBefore("<authentication></authentication>")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example".toUserEmailAddress())

        assertThat(result).isInstanceOf<Settings>()
            .prop(Settings::incomingServerSettings).containsExactly(
                ImapServerSettings(
                    hostname = "imap.domain.example".toHostname(),
                    port = 993.toPort(),
                    connectionSecurity = TLS,
                    authenticationTypes = listOf(PasswordCleartext),
                    username = "user@domain.example",
                ),
            )
    }

    @Test
    fun `config with missing 'emailProvider id' attribute should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").removeAttr("id")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'emailProvider.id' attribute")
    }

    @Test
    fun `config with invalid 'emailProvider id' attribute should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").attr("id", "-23")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Invalid 'emailProvider.id' attribute")
    }

    @Test
    fun `config with missing domain element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider > domain").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Valid 'domain' element required")
    }

    @Test
    fun `config with only invalid domain elements should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider > domain").text("-invalid")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Valid 'domain' element required")
    }

    @Test
    fun `config with missing 'incomingServer' element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("No supported 'incomingServer' element found")
    }

    @Test
    fun `config with missing 'outgoingServer' element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("outgoingServer").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("No supported 'outgoingServer' element found")
    }

    @Test
    fun `incomingServer with missing hostname should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'hostname' element")
    }

    @Test
    fun `incomingServer with invalid hostname should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").text("in valid")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Invalid 'hostname' value: 'in valid'")
    }

    @Test
    fun `incomingServer with missing port should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'port' element")
    }

    @Test
    fun `incomingServer with missing socketType should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > socketType").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'socketType' element")
    }

    @Test
    fun `incomingServer with missing authentication should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > authentication").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("No usable 'authentication' element found")
    }

    @Test
    fun `incomingServer with missing username should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > username").remove()
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'username' element")
    }

    @Test
    fun `incomingServer with invalid port should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").text("invalid")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Invalid 'port' value: 'invalid'")
    }

    @Test
    fun `incomingServer with out of range port number should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").text("100000")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Invalid 'port' value: '100000'")
    }

    @Test
    fun `incomingServer with unknown socketType should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > socketType").text("TLS")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Unknown 'socketType' value: 'TLS'")
    }

    @Test
    fun `element found when expecting text should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").html("imap.domain.example<element/>")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Expected text, but got START_TAG")
    }

    @Test
    fun `ignore 'incomingServer' and 'outgoingServer' inside wrong element`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").tagName("madeUpTag")
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'emailProvider' element")
    }

    @Test
    fun `ignore 'incomingServer' inside unsupported 'incomingServer' element`() {
        val inputStream = minimalConfig.withModifications {
            val incomingServer = element("incomingServer")
            val incomingServerXml = incomingServer.outerHtml()
            incomingServer.attr("type", "unsupported")
            incomingServer.html(incomingServerXml)
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("No supported 'incomingServer' element found")
    }

    @Test
    fun `ignore 'outgoingServer' inside unsupported 'outgoingServer' element`() {
        val inputStream = minimalConfig.withModifications {
            val outgoingServer = element("outgoingServer")
            val outgoingServerXml = outgoingServer.outerHtml()
            outgoingServer.attr("type", "unsupported")
            outgoingServer.html(outgoingServerXml)
        }

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("No supported 'outgoingServer' element found")
    }

    @Test
    fun `non XML data should throw`() {
        val inputStream = "invalid".byteInputStream()

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Error parsing Autoconfig XML")
    }

    @Test
    fun `wrong root element should throw`() {
        @Language("XML")
        val inputStream =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <serverConfig></serverConfig>
            """.trimIndent().byteInputStream()

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Missing 'clientConfig' element")
    }

    @Test
    fun `syntactically incorrect XML should throw`() {
        @Language("XML")
        val inputStream =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <clientConfig version="1.1">
              <emailProvider id="domain.example">
                <incomingServer type="imap">
                  <hostname>imap.domain.example</hostname>
                  <port>993</port>
                  <socketType>SSL</socketType>
                  <authentication>password-cleartext</authentication>
                  <username>%EMAILADDRESS%</username>
                </incomingServer>
                <outgoingServer type="smtp">
                  <hostname>smtp.domain.example</hostname>
                  <port>465</port>
                  <socketType>SSL</socketType>
                  <authentication>password-cleartext</authentication>
                  <username>%EMAILADDRESS%</username>
                </outgoingServer>
              <!-- Missing </emailProvider> -->
            </clientConfig>
            """.trimIndent().byteInputStream()

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("Error parsing Autoconfig XML")
    }

    @Test
    fun `incomplete XML should throw`() {
        @Language("XML")
        val inputStream =
            """
            <?xml version="1.0"?>
            <clientConfig version="1.1">
              <emailProvider id="domain.example">
            """.trimIndent().byteInputStream()

        val result = parser.parseSettings(inputStream, irrelevantEmailAddress)

        assertThat(result).isInstanceOf<ParserError>()
            .prop(ParserError::error).hasMessage("End of document reached while reading element 'emailProvider'")
    }

    private fun String.withModifications(block: Document.() -> Unit): InputStream {
        return Jsoup.parse(this, "", Parser.xmlParser())
            .apply(block)
            .toString()
            .also {
                if (PRINT_MODIFIED_XML) {
                    println(it)
                }
            }
            .byteInputStream()
    }

    private fun Document.element(query: String): Element {
        return select(query).first() ?: error("Couldn't find element using '$query'")
    }

    private fun Element.insertBefore(xml: String) {
        val index = siblingIndex()
        parent()!!.apply {
            append(xml)
            val newElement = lastElementChild()!!
            newElement.remove()
            insertChildren(index, newElement)
        }
    }
}
