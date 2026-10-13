package app.k9mail.feature.account.oauth.data

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
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
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
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
    fun `Zoho authorization request should use granular scopes and fresh security values`() = runTest {
        val callbackUri = "net.thunderbird.android.debug://oauth2redirect"
        val mailboxAddress = "user@example.invalid"
        val configuration = OAuthConfiguration(
            clientId = "user-configured-client",
            scopes = ZohoMailServerSettings.OAUTH_SCOPES,
            authorizationEndpoint = ZohoMailServerSettings.AUTHORIZATION_ENDPOINT,
            tokenEndpoint = ZohoMailServerSettings.TOKEN_ENDPOINT,
            redirectUri = callbackUri,
            scopeSeparator = ZohoMailServerSettings.SCOPE_SEPARATOR,
            authorizationParameters = mapOf("access_type" to "offline", "prompt" to "consent"),
        )
        val authorizationRequests = mutableListOf<AuthorizationRequest>()
        val intent = Intent()
        service.stub {
            on { getAuthorizationRequestIntent(any()) } doAnswer { invocation ->
                authorizationRequests += invocation.getArgument(0, AuthorizationRequest::class.java)
                intent
            }
        }
        val testSubject = AuthorizationRepository(service)

        val firstResult = testSubject.getAuthorizationRequestIntent(configuration, mailboxAddress)
        val secondResult = testSubject.getAuthorizationRequestIntent(configuration, mailboxAddress)

        assertThat(firstResult).isEqualTo(AuthorizationIntentResult.Success(intent))
        assertThat(secondResult).isEqualTo(AuthorizationIntentResult.Success(intent))
        val firstRequest = authorizationRequests[0]
        val secondRequest = authorizationRequests[1]
        assertThat(firstRequest.clientId).isEqualTo("user-configured-client")
        assertThat(firstRequest.responseType).isEqualTo(ResponseTypeValues.CODE)
        assertThat(firstRequest.redirectUri).isEqualTo(callbackUri.toUri())
        assertThat(firstRequest.scope).isEqualTo(ZohoMailServerSettings.OAUTH_SCOPES.joinToString(","))
        assertThat(firstRequest.loginHint).isEqualTo(mailboxAddress)
        assertThat(firstRequest.prompt).isEqualTo("consent")
        assertThat(firstRequest.additionalParameters).isEqualTo(mapOf("access_type" to "offline"))
        assertThat(firstRequest.codeVerifierChallengeMethod).isEqualTo("S256")
        assertThat(firstRequest.state).isNotEqualTo(secondRequest.state)
        assertThat(firstRequest.nonce).isNotEqualTo(secondRequest.nonce)
        assertThat(firstRequest.codeVerifier).isNotEqualTo(secondRequest.codeVerifier)
        assertThat(firstRequest.codeVerifierChallenge).isNotEqualTo(secondRequest.codeVerifierChallenge)
    }

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
                prop(AuthorizationRequest::prompt).isEqualTo("consent")
                prop(AuthorizationRequest::additionalParameters).isEqualTo(mapOf("access_type" to "offline"))
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
            on { performTokenRequest(any(), any()) } doAnswer { invocation ->
                val callback = invocation.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(tokenResponse, null)
            }
        }

        val result = testSubject.getExchangeToken(authorizationResponse)

        val expectedAuthState = AuthState(authorizationResponse, tokenResponse, null)
        val successAuthorizationState = expectedAuthState.toAuthorizationState()

        assertThat(result).isEqualTo(AuthorizationResult.Success(successAuthorizationState))
    }

    @Test
    fun `Zoho token exchange uses callback Accounts server and preserves state and PKCE`() = runTest {
        val response = zohoAuthorizationResponse()
        var capturedRequest: TokenRequest? = null
        service.stub {
            on { performTokenRequest(any(), any()) } doAnswer { invocation ->
                capturedRequest = invocation.getArgument(0, TokenRequest::class.java)
                val callback = invocation.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(TokenResponse.Builder(capturedRequest!!).build(), null)
            }
        }
        val testSubject = AuthorizationRepository(service)

        val result = testSubject.getExchangeToken(response)

        assertThat(result is AuthorizationResult.Success).isEqualTo(true)
        val tokenRequest = requireNotNull(capturedRequest)
        assertThat(tokenRequest.configuration.tokenEndpoint)
            .isEqualTo("https://accounts.zoho.in/oauth/v2/token".toUri())
        assertThat(tokenRequest.authorizationCode).isEqualTo("authorization-code")
        assertThat(tokenRequest.redirectUri).isEqualTo(response.request.redirectUri)
        assertThat(tokenRequest.codeVerifier).isEqualTo(response.request.codeVerifier)
        assertThat(tokenRequest.nonce).isEqualTo(response.request.nonce)

        val authorizationState = (result as AuthorizationResult.Success).state
        val authState = AuthState.jsonDeserialize(requireNotNull(authorizationState.value))
        assertThat(authState.lastAuthorizationResponse?.state).isEqualTo(response.state)
        assertThat(authState.lastTokenResponse?.request?.configuration?.tokenEndpoint)
            .isEqualTo("https://accounts.zoho.in/oauth/v2/token".toUri())
    }

    @Test
    fun `Zoho token exchange rejects missing unknown unsafe or contradictory regional server data`() = runTest {
        var tokenRequestCount = 0
        service.stub {
            on { performTokenRequest(any(), any()) } doAnswer { invocation ->
                tokenRequestCount++
                val callback = invocation.getArgument(1, TokenResponseCallback::class.java)
                val request = invocation.getArgument(0, TokenRequest::class.java)
                callback.onTokenRequestCompleted(TokenResponse.Builder(request).build(), null)
            }
        }
        val testSubject = AuthorizationRepository(service)
        val invalidResponses = listOf(
            zohoAuthorizationResponse(accountsServer = null),
            zohoAuthorizationResponse(accountsServer = "https://accounts.zoho.in.attacker.invalid"),
            zohoAuthorizationResponse(accountsServer = "http://accounts.zoho.in"),
            zohoAuthorizationResponse(location = "eu", accountsServer = "https://accounts.zoho.in"),
            zohoAuthorizationResponse(location = "unknown", accountsServer = "https://accounts.zoho.in"),
        )

        invalidResponses.forEach { response ->
            val result = testSubject.getExchangeToken(response)
            assertThat(result is AuthorizationResult.Failure).isEqualTo(true)
        }
        assertThat(tokenRequestCount).isEqualTo(0)
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
            on { performTokenRequest(any(), any()) } doAnswer { invocation ->
                val callback = invocation.getArgument(1, TokenResponseCallback::class.java)
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
            on { performTokenRequest(any(), any()) } doAnswer { invocation ->
                val callback = invocation.getArgument(1, TokenResponseCallback::class.java)
                callback.onTokenRequestCompleted(null, null)
            }
        }

        val result = testSubject.getExchangeToken(authorizationResponse)

        assertThat(result).isEqualTo(AuthorizationResult.Failure(exception))
    }

    private companion object {
        fun zohoAuthorizationResponse(
            location: String? = "in",
            accountsServer: String? = "https://accounts.zoho.in",
        ): AuthorizationResponse {
            val configuration = AuthorizationServiceConfiguration(
                ZohoMailServerSettings.AUTHORIZATION_ENDPOINT.toUri(),
                ZohoMailServerSettings.TOKEN_ENDPOINT.toUri(),
            )
            val request = AuthorizationRequest.Builder(
                configuration,
                "configured-zoho-client",
                ResponseTypeValues.CODE,
                "net.thunderbird.android://oauth2redirect".toUri(),
            )
                .setState("test-oauth-state")
                .setCodeVerifier("0123456789abcdefghijklmnopqrstuvwxyzABCDEFG")
                .build()
            val additionalParameters = buildMap {
                location?.let { put("location", it) }
                accountsServer?.let { put("accounts-server", it) }
            }
            return AuthorizationResponse.Builder(request)
                .setState("test-oauth-state")
                .setAuthorizationCode("authorization-code")
                .setAdditionalParameters(additionalParameters)
                .build()
        }

        val oAuthConfiguration = OAuthConfiguration(
            clientId = "clientId",
            scopes = listOf("scope", "scope2"),
            authorizationEndpoint = "auth.example.com",
            tokenEndpoint = "token.example.com",
            redirectUri = "redirect.example.com",
            authorizationParameters = mapOf("access_type" to "offline", "prompt" to "consent"),
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
