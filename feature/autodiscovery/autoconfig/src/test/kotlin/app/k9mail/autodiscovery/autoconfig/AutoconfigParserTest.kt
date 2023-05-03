package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.DiscoveredServerSettings
import app.k9mail.autodiscovery.api.DiscoveryResults
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import java.io.InputStream
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.junit.Test

private const val PRINT_MODIFIED_XML = false

class AutoconfigParserTest {
    private val parser = AutoconfigParser()

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

    @Test
    fun `minimal data`() {
        val inputStream = minimalConfig.byteInputStream()

        val result = parser.parseSettings(inputStream, email = "user@domain.example")

        assertThat(result).isNotNull().all {
            prop(DiscoveryResults::incoming).containsExactly(
                DiscoveredServerSettings(
                    protocol = "imap",
                    host = "imap.domain.example",
                    port = 993,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authType = AuthType.PLAIN,
                    username = "user@domain.example",
                ),
            )
            prop(DiscoveryResults::outgoing).containsExactly(
                DiscoveredServerSettings(
                    protocol = "smtp",
                    host = "smtp.domain.example",
                    port = 587,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    authType = AuthType.PLAIN,
                    username = "user@domain.example",
                ),
            )
        }
    }

    @Test
    fun `real-world data`() {
        val inputStream = javaClass.getResourceAsStream("/2022-11-19-googlemail.com.xml")!!

        val result = parser.parseSettings(inputStream, email = "test@gmail.com")

        assertThat(result).isNotNull().all {
            prop(DiscoveryResults::incoming).containsExactly(
                DiscoveredServerSettings(
                    protocol = "imap",
                    host = "imap.gmail.com",
                    port = 993,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authType = AuthType.XOAUTH2,
                    username = "test@gmail.com",
                ),
            )
            prop(DiscoveryResults::outgoing).containsExactly(
                DiscoveredServerSettings(
                    protocol = "smtp",
                    host = "smtp.gmail.com",
                    port = 465,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authType = AuthType.XOAUTH2,
                    username = "test@gmail.com",
                ),
            )
        }
    }

    @Test
    fun `replace variables`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").text("%EMAILLOCALPART%.domain.example")
            element("outgoingServer > hostname").text("%EMAILLOCALPART%.outgoing.domain.example")
            element("outgoingServer > username").text("%EMAILDOMAIN%")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example")

        assertThat(result).isNotNull().all {
            prop(DiscoveryResults::incoming).containsExactly(
                DiscoveredServerSettings(
                    protocol = "imap",
                    host = "user.domain.example",
                    port = 993,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authType = AuthType.PLAIN,
                    username = "user@domain.example",
                ),
            )
            prop(DiscoveryResults::outgoing).containsExactly(
                DiscoveredServerSettings(
                    protocol = "smtp",
                    host = "user.outgoing.domain.example",
                    port = 587,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    authType = AuthType.PLAIN,
                    username = "domain.example",
                ),
            )
        }
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

        val result = parser.parseSettings(inputStream, email = "user@domain.example")

        assertThat(result.incoming).containsExactly(
            DiscoveredServerSettings(
                protocol = "imap",
                host = "imap.domain.example",
                port = 993,
                security = ConnectionSecurity.SSL_TLS_REQUIRED,
                authType = AuthType.PLAIN,
                username = "user@domain.example",
            ),
        )
    }

    @Test
    fun `empty authentication element should be ignored`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > authentication").insertBefore("<authentication></authentication>")
        }

        val result = parser.parseSettings(inputStream, email = "user@domain.example")

        assertThat(result.incoming).containsExactly(
            DiscoveredServerSettings(
                protocol = "imap",
                host = "imap.domain.example",
                port = 993,
                security = ConnectionSecurity.SSL_TLS_REQUIRED,
                authType = AuthType.PLAIN,
                username = "user@domain.example",
            ),
        )
    }

    @Test
    fun `config with missing 'emailProvider id' attribute should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").removeAttr("id")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'emailProvider.id' attribute")
    }

    @Test
    fun `config with invalid 'emailProvider id' attribute should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").attr("id", "-23")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Invalid 'emailProvider.id' attribute")
    }

    @Test
    fun `config with missing domain element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider > domain").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Valid 'domain' element required")
    }

    @Test
    fun `config with only invalid domain elements should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider > domain").text("-invalid")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Valid 'domain' element required")
    }

    @Test
    fun `config with missing 'incomingServer' element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'incomingServer' element")
    }

    @Test
    fun `config with missing 'outgoingServer' element should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("outgoingServer").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'outgoingServer' element")
    }

    @Test
    fun `incomingServer with missing hostname should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'hostname' element")
    }

    @Test
    fun `incomingServer with invalid hostname should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").text("in valid")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Invalid 'hostname' value: 'in valid'")
    }

    @Test
    fun `incomingServer with missing port should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'port' element")
    }

    @Test
    fun `incomingServer with missing socketType should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > socketType").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'socketType' element")
    }

    @Test
    fun `incomingServer with missing authentication should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > authentication").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("No usable 'authentication' element found")
    }

    @Test
    fun `incomingServer with missing username should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > username").remove()
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'username' element")
    }

    @Test
    fun `incomingServer with invalid port should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").text("invalid")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Invalid 'port' value: 'invalid'")
    }

    @Test
    fun `incomingServer with out of range port number should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > port").text("100000")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Invalid 'port' value: '100000'")
    }

    @Test
    fun `incomingServer with unknown socketType should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > socketType").text("TLS")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Unknown 'socketType' value: 'TLS'")
    }

    @Test
    fun `element found when expecting text should throw`() {
        val inputStream = minimalConfig.withModifications {
            element("incomingServer > hostname").html("imap.domain.example<element/>")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Expected text, but got START_TAG")
    }

    @Test
    fun `ignore 'incomingServer' and 'outgoingServer' inside wrong element`() {
        val inputStream = minimalConfig.withModifications {
            element("emailProvider").tagName("madeUpTag")
        }

        assertThat {
            parser.parseSettings(inputStream, email = "user@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'emailProvider' element")
    }

    @Test
    fun `non XML data should throw`() {
        val inputStream = "invalid".byteInputStream()

        assertThat {
            parser.parseSettings(inputStream, email = "irrelevant@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Error parsing Autoconfig XML")
    }

    @Test
    fun `wrong root element should throw`() {
        @Language("XML")
        val inputStream =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <serverConfig></serverConfig>
            """.trimIndent().byteInputStream()

        assertThat {
            parser.parseSettings(inputStream, email = "irrelevant@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Missing 'clientConfig' element")
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

        assertThat {
            parser.parseSettings(inputStream, email = "irrelevant@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("Error parsing Autoconfig XML")
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

        assertThat {
            parser.parseSettings(inputStream, email = "irrelevant@domain.example")
        }.isFailure()
            .isInstanceOf(AutoconfigParserException::class)
            .hasMessage("End of document reached while reading element 'emailProvider'")
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
