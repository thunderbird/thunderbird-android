package app.k9mail.feature.account.oauth.data

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizationRepositoryTest {

    private val service: AuthorizationService = mock<AuthorizationService>()

    @Test
    fun `getAuthorizationRequestIntent should return Success with intent when hostname has oauth configuration`() =
        runTest {
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
                prop(AuthorizationRequest::loginHint).isEqualTo(emailAddress)
                prop(AuthorizationRequest::codeVerifier).isNotNull()
                prop(AuthorizationRequest::codeVerifierChallengeMethod).isEqualTo("S256")
                prop(AuthorizationRequest::codeVerifierChallenge).isNotNull().isNotEmpty()
            }
        }

    @Test
    fun `getAuthorizationResponse should return AuthorizationResponse when intent invalid`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val intent = Intent().apply {
            putExtra(AuthorizationResponse.EXTRA_RESPONSE, authorizationResponse.jsonSerializeString())
        }

        // When
        val result = testSubject.getAuthorizationResponse(intent)

        // Then
        assertThat(result).isNotNull().all {
            prop(AuthorizationResponse::request).all {
                prop(AuthorizationRequest::configuration).all {
                    prop(AuthorizationServiceConfiguration::authorizationEndpoint).isEqualTo(
                        authorizationConfiguration.authorizationEndpoint,
                    )
                    prop(AuthorizationServiceConfiguration::tokenEndpoint).isEqualTo(
                        authorizationConfiguration.tokenEndpoint,
                    )
                }
                prop(AuthorizationRequest::clientId).isEqualTo(authorizationRequest.clientId)
                prop(AuthorizationRequest::responseType).isEqualTo(authorizationRequest.responseType)
                prop(AuthorizationRequest::redirectUri).isEqualTo(authorizationRequest.redirectUri)
            }
        }
    }

    @Test
    fun `getAuthorizationResponse should return null when intent is invalid`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val intent = Intent()

        // When
        val result = testSubject.getAuthorizationResponse(intent)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getAuthorizationException should return AuthorizationException when intent is valid`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val authorizationException = AuthorizationException(
            AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR,
            1,
            "error",
            "errorDescription",
            Uri.parse("https://example.com/errorUri"),
            null,
        )
        val intent = Intent().apply {
            putExtra(AuthorizationException.EXTRA_EXCEPTION, authorizationException.toJsonString())
        }

        // When
        val result = testSubject.getAuthorizationException(intent)

        // Then
        assertThat(result).isEqualTo(authorizationException)
    }

    @Test
    fun `getAuthorizationException should return null when intent is invalid`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val intent = Intent()

        // When
        val result = testSubject.getAuthorizationException(intent)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getExchangeToken should return success when tokenRequest successful`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val tokenRequest = TokenRequest.Builder(
            authorizationConfiguration,
            authorizationRequest.clientId,
        ).setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setAuthorizationCode("authorizationCode")
            .setRedirectUri(authorizationRequest.redirectUri)
            .build()
        val tokenResponse = TokenResponse.Builder(tokenRequest)
            .build()
        service.stub {
            on { performTokenRequest(any(), any()) } doAnswer {
                val callback = it.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(tokenResponse, null)
            }
        }

        val result = testSubject.getExchangeToken(authorizationResponse)

        val expectedAuthState = AuthState(authorizationResponse, tokenResponse, null)
        val successAuthorizationState = expectedAuthState.toAuthorizationState()

        assertThat(result).isEqualTo(AuthorizationResult.Success(successAuthorizationState))
    }

    fun `getExchangeToken should return failure when tokenRequest failure`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val authorizationException = AuthorizationException(
            AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR,
            1,
            "error",
            "errorDescription",
            Uri.parse("https://example.com/errorUri"),
            null,
        )
        service.stub {
            on { performTokenRequest(any(), any()) } doAnswer {
                val callback = it.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(null, authorizationException)
            }
        }

        val result = testSubject.getExchangeToken(authorizationResponse)

        assertThat(result).isEqualTo(AuthorizationResult.Failure(authorizationException))
    }

    fun `getExchangeToken should return unknown failure when tokenRequest null for response and exception`() = runTest {
        val testSubject = AuthorizationRepository(
            service = service,
        )
        val exception = Exception("Unknown error")
        service.stub {
            on { performTokenRequest(any(), any()) } doAnswer {
                val callback = it.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(null, null)
            }
        }

        val result = testSubject.getExchangeToken(authorizationResponse)

        assertThat(result).isEqualTo(AuthorizationResult.Failure(exception))
    }

    private companion object {
        val oAuthConfiguration = OAuthConfiguration(
            clientId = "clientId",
            scopes = listOf("scope", "scope2"),
            authorizationEndpoint = "auth.example.com",
            tokenEndpoint = "token.example.com",
            redirectUri = "redirect.example.com",
        )

        val authorizationConfiguration = AuthorizationServiceConfiguration(
            Uri.parse("https://example.com/authorize"),
            Uri.parse("https://example.com/token"),
        )

        val authorizationRequest = AuthorizationRequest.Builder(
            authorizationConfiguration,
            "clientId",
            "responseType",
            Uri.parse("https://example.com/redirectUri"),
        ).build()

        val authorizationResponse = AuthorizationResponse.Builder(authorizationRequest)
            .setAuthorizationCode("authorizationCode")
            .build()
    }
}
