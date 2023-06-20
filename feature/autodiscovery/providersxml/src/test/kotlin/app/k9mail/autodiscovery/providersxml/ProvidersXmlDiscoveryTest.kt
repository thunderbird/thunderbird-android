package app.k9mail.autodiscovery.providersxml

import androidx.test.core.app.ApplicationProvider
import app.k9mail.core.android.testing.RobolectricTest
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import org.junit.Test

class ProvidersXmlDiscoveryTest : RobolectricTest() {
    private val xmlProvider = ProvidersXmlProvider(ApplicationProvider.getApplicationContext())
    private val oAuthConfigurationProvider = createOAuthConfigurationProvider()
    private val providersXmlDiscovery = ProvidersXmlDiscovery(xmlProvider, oAuthConfigurationProvider)

    @Test
    fun discover_withGmailDomain_shouldReturnCorrectSettings() {
        val connectionSettings = providersXmlDiscovery.discover("user@gmail.com")

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("imap.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
    }

    @Test
    fun discover_withUnknownDomain_shouldReturnNull() {
        val connectionSettings = providersXmlDiscovery.discover(
            "user@not.present.in.providers.xml.example",
        )

        assertThat(connectionSettings).isNull()
    }

    private fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
        return object : OAuthConfigurationProvider {
            override fun getConfiguration(hostname: String): OAuthConfiguration? {
                return when (hostname) {
                    "imap.gmail.com" -> oAuthConfiguration
                    "smtp.gmail.com" -> oAuthConfiguration
                    else -> null
                }
            }
        }
    }

    private val oAuthConfiguration = OAuthConfiguration(
        clientId = "irrelevant",
        scopes = listOf("irrelevant"),
        authorizationEndpoint = "irrelevant",
        tokenEndpoint = "irrelevant",
        redirectUri = "irrelevant",
    )
}
