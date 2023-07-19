package app.k9mail.feature.account.oauth.data

import android.content.Intent
import androidx.core.net.toUri
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizationRepositoryTest {

    private val service: AuthorizationService = mock<AuthorizationService>()

    @Test
    fun `should return Success with intent when hostname has oauth configuration`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val emailAddress = "emailAddress"
        val intent = Intent()
        val authRequestCapture = argumentCaptor<AuthorizationRequest>().apply {
            service.stub { on { getAuthorizationRequestIntent(capture()) }.thenReturn(intent) }
        }

        // When
        val result = testSubject.getAuthorizationRequestIntent(oAuthConfiguration, emailAddress)

        // Then
        assertThat(result).isEqualTo(
            AuthorizationIntentResult.Success(
                intent = intent,
            ),
        )
        assertThat(authRequestCapture.firstValue).all {
            prop(AuthorizationRequest::configuration).all {
                prop(AuthorizationServiceConfiguration::authorizationEndpoint).isEqualTo(
                    oAuthConfiguration.authorizationEndpoint.toUri(),
                )
                prop(AuthorizationServiceConfiguration::tokenEndpoint).isEqualTo(
                    oAuthConfiguration.tokenEndpoint.toUri(),
                )
            }
            prop(AuthorizationRequest::clientId).isEqualTo(oAuthConfiguration.clientId)
            prop(AuthorizationRequest::responseType).isEqualTo(ResponseTypeValues.CODE)
            prop(AuthorizationRequest::redirectUri).isEqualTo(oAuthConfiguration.redirectUri.toUri())
            prop(AuthorizationRequest::scope).isEqualTo("scope scope2")
            prop(AuthorizationRequest::codeVerifier).isNull()
            prop(AuthorizationRequest::loginHint).isEqualTo(emailAddress)
        }
    }

    private companion object {
        val oAuthConfiguration = OAuthConfiguration(
            clientId = "clientId",
            scopes = listOf("scope", "scope2"),
            authorizationEndpoint = "auth.example.com",
            tokenEndpoint = "token.example.com",
            redirectUri = "redirect.example.com",
        )
    }
}
