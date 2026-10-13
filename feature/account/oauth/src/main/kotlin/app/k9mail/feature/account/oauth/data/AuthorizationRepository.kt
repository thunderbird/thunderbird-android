package app.k9mail.feature.account.oauth.data

import android.content.Intent
import androidx.core.net.toUri
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import java.net.URI
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.legacy.logging.Log

class AuthorizationRepository(
    private val service: AuthorizationService,
) : AccountOAuthDomainContract.AuthorizationRepository {

    override fun getAuthorizationRequestIntent(
        configuration: OAuthConfiguration,
        emailAddress: String,
    ): AuthorizationIntentResult {
        return AuthorizationIntentResult.Success(
            createAuthorizationRequestIntent(configuration, emailAddress),
        )
    }

    override suspend fun getAuthorizationResponse(intent: Intent): AuthorizationResponse? {
        return try {
            AuthorizationResponse.fromIntent(intent)
        } catch (e: IllegalArgumentException) {
            Log.e(e, "Error deserializing AuthorizationResponse")
            null
        }
    }

    override suspend fun getAuthorizationException(intent: Intent): AuthorizationException? {
        return try {
            AuthorizationException.fromIntent(intent)
        } catch (e: IllegalArgumentException) {
            Log.e(e, "Error deserializing AuthorizationException")
            null
        }
    }

    override suspend fun getExchangeToken(response: AuthorizationResponse): AuthorizationResult =
        getExchangeToken(response, null)

    override suspend fun getExchangeToken(
        response: AuthorizationResponse,
        clientSecret: String?,
    ): AuthorizationResult {
        val tokenRequest = try {
            createTokenExchangeRequest(response)
        } catch (exception: IllegalArgumentException) {
            return AuthorizationResult.Failure(exception)
        }
        val clientAuthentication = clientSecret?.takeIf(String::isNotBlank)?.let(::ClientSecretPost)

        return suspendCoroutine { continuation ->
            val callback = AuthorizationService.TokenResponseCallback { tokenResponse, authorizationException ->
                val result = if (authorizationException != null) {
                    AuthorizationResult.Failure(authorizationException)
                } else if (tokenResponse != null) {
                    val authState = AuthState(response, tokenResponse, null)
                    AuthorizationResult.Success(authState.toAuthorizationState())
                } else {
                    AuthorizationResult.Failure(Exception("Unknown error"))
                }

                continuation.resume(result)
            }

            if (clientAuthentication == null) {
                service.performTokenRequest(tokenRequest, callback)
            } else {
                service.performTokenRequest(tokenRequest, clientAuthentication, callback)
            }
        }
    }

    private fun createTokenExchangeRequest(response: AuthorizationResponse): TokenRequest {
        val standardRequest = response.createTokenExchangeRequest()
        if (response.request.configuration.authorizationEndpoint !=
            ZohoMailServerSettings.AUTHORIZATION_ENDPOINT.toUri()
        ) {
            return standardRequest
        }

        val regionalConfiguration = AuthorizationServiceConfiguration(
            response.request.configuration.authorizationEndpoint,
            resolveZohoTokenEndpoint(response),
        )
        return TokenRequest.Builder(regionalConfiguration, standardRequest.clientId)
            .setGrantType(standardRequest.grantType)
            .setRedirectUri(standardRequest.redirectUri)
            .setAuthorizationCode(standardRequest.authorizationCode)
            .apply {
                standardRequest.codeVerifier?.let(::setCodeVerifier)
                standardRequest.nonce?.let(::setNonce)
            }
            .setAdditionalParameters(standardRequest.additionalParameters)
            .build()
    }

    private fun resolveZohoTokenEndpoint(response: AuthorizationResponse): android.net.Uri {
        val accountsServer = response.additionalParameters[ZOHO_ACCOUNTS_SERVER_PARAMETER]
        require(!accountsServer.isNullOrBlank()) {
            "Zoho OAuth response did not include a supported Accounts server"
        }
        val serverUri = runCatching { URI(accountsServer) }.getOrNull()
        val host = serverUri?.host?.lowercase(Locale.ROOT)
        require(
            serverUri != null &&
                serverUri.scheme.equals("https", ignoreCase = true) &&
                serverUri.rawUserInfo == null &&
                serverUri.port in setOf(DEFAULT_URI_PORT, HTTPS_PORT) &&
                serverUri.rawPath.orEmpty() in setOf("", "/") &&
                serverUri.rawQuery == null &&
                serverUri.rawFragment == null &&
                host != null &&
                host in ZOHO_ACCOUNTS_HOST_TO_LOCATIONS,
        ) {
            "Zoho OAuth response did not include a supported Accounts server"
        }

        val location = response.additionalParameters[ZOHO_LOCATION_PARAMETER]
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.removePrefix(".")
        if (location != null) {
            require(ZOHO_LOCATION_TO_ACCOUNTS_HOST[location] == host) {
                "Zoho OAuth response location did not match its Accounts server"
            }
        }

        return "https://$host/oauth/v2/token".toUri()
    }

    private fun createAuthorizationRequestIntent(configuration: OAuthConfiguration, emailAddress: String): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            configuration.authorizationEndpoint.toUri(),
            configuration.tokenEndpoint.toUri(),
        )

        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            configuration.clientId,
            ResponseTypeValues.CODE,
            configuration.redirectUri.toUri(),
        )

        val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()

        val authRequest = authRequestBuilder
            .setScope(configuration.scopes.joinToString(configuration.scopeSeparator))
            .setCodeVerifier(codeVerifier)
            .setAdditionalParameters(configuration.authorizationParameters - "prompt")
            .apply {
                configuration.authorizationParameters["prompt"]?.let(::setPrompt)
                if (emailAddress.isNotBlank()) {
                    setLoginHint(emailAddress)
                }
            }
            .build()

        return service.getAuthorizationRequestIntent(authRequest)
    }

    private companion object {
        const val ZOHO_ACCOUNTS_SERVER_PARAMETER = "accounts-server"
        const val ZOHO_LOCATION_PARAMETER = "location"
        const val DEFAULT_URI_PORT = -1
        const val HTTPS_PORT = 443

        val ZOHO_LOCATION_TO_ACCOUNTS_HOST = mapOf(
            "us" to "accounts.zoho.com",
            "com" to "accounts.zoho.com",
            "eu" to "accounts.zoho.eu",
            "in" to "accounts.zoho.in",
            "au" to "accounts.zoho.com.au",
            "com.au" to "accounts.zoho.com.au",
            "jp" to "accounts.zoho.jp",
            "ca" to "accounts.zohocloud.ca",
            "cn" to "accounts.zoho.com.cn",
            "com.cn" to "accounts.zoho.com.cn",
            "ae" to "accounts.zoho.ae",
            "sa" to "accounts.zoho.sa",
        )
        val ZOHO_ACCOUNTS_HOST_TO_LOCATIONS = ZOHO_LOCATION_TO_ACCOUNTS_HOST.values.toSet()
    }
}
