package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationException.AuthorizationRequestErrors
import net.openid.appauth.AuthorizationException.GeneralErrors
import net.openid.appauth.AuthorizationService
import net.thunderbird.core.logging.legacy.Log

class RealOAuth2TokenProvider(
    context: Context,
    private val authStateStorage: AuthStateStorage,
) : OAuth2TokenProvider {
    private val authService = AuthorizationService(context)
    private var requestFreshToken = false

    override val usernames: Set<String>
        get() {
            val idTokenClaims = parseAuthState().parsedIdToken?.additionalClaims.orEmpty()
            return buildSet {
                // https://learn.microsoft.com/en-us/entra/identity-platform/id-token-claims-reference#payload-claims
                // https://docs.azure.cn/en-us/entra/identity-platform/optional-claims-reference
                // requires profile scope
                idTokenClaims["preferred_username"]?.let { add(it.toString()) }
                // requires email scope
                idTokenClaims["email"]?.let { add(it.toString()) }
                // only present for v1.0 tokens
                idTokenClaims["unique_name"]?.let { add(it.toString()) }
                // requires profile scope
                idTokenClaims["upn"]?.let { add(it.toString()) }
                idTokenClaims["verified_primary_email"]?.let { verifiedPrimaryEmail ->
                    when (verifiedPrimaryEmail) {
                        is List<*> -> addAll(verifiedPrimaryEmail.map { it.toString() })
                        else -> add(verifiedPrimaryEmail.toString())
                    }
                }
                idTokenClaims["verified_secondary_email"]?.let { verifiedSecondaryEmail ->
                    when (verifiedSecondaryEmail) {
                        is List<*> -> addAll(verifiedSecondaryEmail.map { it.toString() })
                        else -> add(verifiedSecondaryEmail.toString())
                    }
                }
            }
        }

    @Suppress("TooGenericExceptionCaught")
    override fun getToken(timeoutMillis: Long): String {
        val latch = CountDownLatch(1)
        var token: String? = null
        var exception: AuthorizationException? = null

        val authState = parseAuthState()
        if (requestFreshToken) {
            authState.needsTokenRefresh = true
        }

        val oldAccessToken = authState.accessToken

        try {
            authState.performActionWithFreshTokens(
                authService,
            ) { accessToken: String?, _, authException: AuthorizationException? ->
                token = accessToken
                exception = authException

                latch.countDown()
            }

            latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.w(e, "Failed to fetch an access token. Clearing authorization state.")

            authStateStorage.updateAuthorizationState(authorizationState = null)

            throw AuthenticationFailedException(
                message = "Failed to fetch an access token",
                throwable = e,
            )
        }

        val authException = exception
        if (authException == GeneralErrors.NETWORK_ERROR ||
            authException == GeneralErrors.SERVER_ERROR ||
            authException == AuthorizationRequestErrors.SERVER_ERROR ||
            authException == AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE
        ) {
            throw IOException("Error while fetching an access token", authException)
        } else if (authException != null) {
            authStateStorage.updateAuthorizationState(authorizationState = null)

            throw AuthenticationFailedException(
                message = "Failed to fetch an access token",
                throwable = authException,
                messageFromServer = authException.error,
            )
        } else if (token != oldAccessToken) {
            requestFreshToken = false
            authStateStorage.updateAuthorizationState(authorizationState = authState.jsonSerializeString())
        }

        return token ?: throw AuthenticationFailedException("Failed to fetch an access token")
    }

    override fun invalidateToken() {
        requestFreshToken = true
    }

    private fun parseAuthState(): AuthState {
        return authStateStorage
            .getAuthorizationState()
            ?.let { AuthState.jsonDeserialize(it) }
            ?: throw AuthenticationFailedException("Login required")
    }
}
