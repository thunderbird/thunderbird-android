package app.k9mail.feature.account.oauth.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.common.mail.Protocols
import org.junit.Test

class NativeApiOAuthConfigurationTest {
    @Test
    fun `Gmail requests approved OIDC profile scopes and keeps Gmail API access`() {
        val configuration = NativeApiOAuthConfiguration.defaultFor(Protocols.GMAIL_API, APP_ID)

        assertThat(configuration?.scopes).isEqualTo(listOf("openid", "profile", "https://mail.google.com/"))
        assertThat(configuration?.redirectUri).isEqualTo("$APP_ID:/oauth2redirect")
    }

    @Test
    fun `Graph requests OIDC identity scopes and app scheme callback`() {
        val configuration = NativeApiOAuthConfiguration.defaultFor(Protocols.GRAPH, APP_ID)

        assertThat(configuration?.scopes)
            .isEqualTo(listOf("openid", "profile", "email", "offline_access", "Mail.ReadWrite", "Mail.Send"))
        assertThat(configuration?.redirectUri).isEqualTo("$APP_ID://oauth2redirect")
    }

    @Test
    fun `does not build callback defaults without an application ID`() {
        assertThat(NativeApiOAuthConfiguration.defaultFor(Protocols.GRAPH, "")).isEqualTo(null)
    }

    private companion object {
        const val APP_ID = "net.thunderbird.android.debug"
    }
}
