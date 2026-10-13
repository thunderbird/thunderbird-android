package app.k9mail.feature.account.oauth.domain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class RegisteredOAuthRedirectUriTest {
    private val applicationId = "net.thunderbird.android.debug"

    @Test
    fun `accepts callbacks matching the app ID scheme`() {
        assertThat(isRegisteredOAuthRedirectUri("$applicationId:/oauth2redirect", applicationId)).isTrue()
        assertThat(isRegisteredOAuthRedirectUri("$applicationId://oauth2redirect", applicationId)).isTrue()
    }

    @Test
    fun `accepts Microsoft callback only when the host is the app ID`() {
        assertThat(isRegisteredOAuthRedirectUri("msauth://$applicationId/eaXDuh%3D", applicationId)).isTrue()
        assertThat(isRegisteredOAuthRedirectUri("msauth://net.other.app/eaXDuh%3D", applicationId)).isFalse()
    }

    @Test
    fun `rejects web callbacks and URIs with query or fragment components`() {
        assertThat(isRegisteredOAuthRedirectUri("https://example.com/callback", applicationId)).isFalse()
        assertThat(isRegisteredOAuthRedirectUri("$applicationId:/oauth2redirect?token=leak", applicationId)).isFalse()
        assertThat(isRegisteredOAuthRedirectUri("$applicationId:/oauth2redirect#token", applicationId)).isFalse()
    }
}
