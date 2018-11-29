package com.fsck.k9.mail.oauth

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountsException
import android.app.Activity
import android.content.Context
import com.fsck.k9.mail.AuthenticationFailedException
import java.io.IOException
import java.util.HashMap

/**
 * An interface between the OAuth2 requirements used for authentication and the AccountManager.
 */
class AndroidAccountOAuth2TokenStore(applicationContext: Context) : OAuth2TokenProvider {

    private val authTokens = HashMap<String, String>()
    private val accountManager: AccountManager

    init {
        this.accountManager = AccountManager.get(applicationContext)
    }

    override fun authorizeApi(username: String, activity: Activity, callback: OAuth2TokenProvider.OAuth2TokenProviderAuthCallback) {
        val account = Account(username, GOOGLE_ACCOUNT_TYPE)
        accountManager.getAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, null,
                activity, AccountManagerCallback { accountManagerFuture ->
            try {
                accountManagerFuture.result
            } catch (e: Exception) {
                callback.failure(AuthorizationException("Authorization failed", e))
                return@AccountManagerCallback
            }

            callback.success(GOOGLE_STORE_SERVER_HOST, GOOGLE_TRANSPORT_SERVER_HOST)
        }, null)
    }

    @Throws(AuthenticationFailedException::class)
    override fun getToken(username: String): String {
        return authTokens[username] ?: fetchNewAuthToken(username)
    }

    override fun getSupportedAccountTypes(): Array<String> {
        return arrayOf(GOOGLE_ACCOUNT_TYPE)
    }

    @Throws(AuthenticationFailedException::class)
    private fun fetchNewAuthToken(username: String): String {
        val account = Account(username, GOOGLE_ACCOUNT_TYPE)
        try {
            val authToken = accountManager
                    .blockingGetAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, false)
                    ?: throw AuthenticationFailedException("Authentication failed")
            authTokens[username] = authToken
            return authToken
        } catch (e: AccountsException) {
            throw AuthenticationFailedException("Request cancelled")
        } catch (e: IOException) {
            throw AuthenticationFailedException("Could not communicate with authenticator")
        }
    }

    override fun invalidateToken(username: String) {
        accountManager.invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, authTokens.remove(username))
    }

    companion object {
        private val GMAIL_AUTH_TOKEN_TYPE = "oauth2:https://mail.google.com/"
        private val GOOGLE_ACCOUNT_TYPE = "com.google"
        private val GOOGLE_STORE_SERVER_HOST = "imap.gmail.com"
        private val GOOGLE_TRANSPORT_SERVER_HOST = "smtp.gmail.com"
    }
}
