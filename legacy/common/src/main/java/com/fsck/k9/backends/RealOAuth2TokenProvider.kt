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
import timber.log.Timber

class RealOAuth2TokenProvider(
    context: Context,
    private val authStateStorage: AuthStateStorage,
) : OAuth2TokenProvider {
    private val authService = AuthorizationService(context)
    private var requestFreshToken = false

    override val primaryEmail: String?
        get() {
            return parseAuthState()
                .parsedIdToken
                ?.additionalClaims
                ?.get("email")
                ?.toString()
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
            Timber.w(e, "Failed to fetch an access token. Clearing authorization state.")

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
