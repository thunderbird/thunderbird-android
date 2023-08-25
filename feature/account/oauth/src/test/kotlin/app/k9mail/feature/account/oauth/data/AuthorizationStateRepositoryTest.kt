package app.k9mail.feature.account.oauth.data

import android.net.Uri
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizationStateRepositoryTest {

    @Test
    fun `should return false with unauthorized auth state`() = runTest {
        val authState = AuthState()
        val authorizationState = authState.toAuthorizationState()
        val testSubject = AuthorizationStateRepository()

        val result = testSubject.isAuthorized(authorizationState)

        assertThat(result).isFalse()
    }

    @Test
    fun `should return true with authorized auth state`() = runTest {
        val authState = AuthState()
        val clientId = "clientId"
        val configuration = AuthorizationServiceConfiguration(
            Uri.parse("https://example.com"),
            Uri.parse("https://example.com"),
        )
        val tokenRequest = TokenRequest.Builder(configuration, clientId)
            .setGrantType(TokenRequest.GRANT_TYPE_PASSWORD)
            .build()
        val tokenResponse = TokenResponse.Builder(tokenRequest)
            .setAccessToken("accessToken")
            .setIdToken("idToken")
            .build()
        authState.update(tokenResponse, null)
        val authorizationState = authState.toAuthorizationState()

        val testSubject = AuthorizationStateRepository()

        val result = testSubject.isAuthorized(authorizationState)

        assertThat(result).isTrue()
    }
}
