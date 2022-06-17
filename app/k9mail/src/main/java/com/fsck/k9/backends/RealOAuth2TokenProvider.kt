package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.preferences.AccountManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService

class RealOAuth2TokenProvider(
    context: Context,
    private val accountManager: AccountManager,
    private val account: Account
) : OAuth2TokenProvider {
    private val authService = AuthorizationService(context)
    private var requestFreshToken = false

    override fun getToken(timeoutMillis: Long): String {
        val latch = CountDownLatch(1)
        var token: String? = null
        var exception: AuthorizationException? = null

        val authState = account.oAuthState?.let { AuthState.jsonDeserialize(it) }
            ?: throw AuthenticationFailedException("Login required")

        if (requestFreshToken) {
            authState.needsTokenRefresh = true
        }

        val oldAccessToken = authState.accessToken

        authState.performActionWithFreshTokens(authService) { accessToken: String?, _, authException: AuthorizationException? ->
            token = accessToken
            exception = authException

            latch.countDown()
        }

        latch.await(timeoutMillis, TimeUnit.MILLISECONDS)

        if (exception != null) {
            account.oAuthState = null
            accountManager.saveAccount(account)
        } else if (token != oldAccessToken) {
            requestFreshToken = false
            account.oAuthState = authState.jsonSerializeString()
            accountManager.saveAccount(account)
        }

        exception?.let { authException ->
            throw AuthenticationFailedException(
                message = "Failed to fetch an access token",
                throwable = authException,
                messageFromServer = authException.error
            )
        }

        return token ?: throw AuthenticationFailedException("Failed to fetch an access token")
    }

    override fun invalidateToken() {
        requestFreshToken = true
    }
}
