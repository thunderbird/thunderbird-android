package com.fsck.k9.mail.oauth

import android.app.Activity
import com.fsck.k9.mail.AuthenticationFailedException

interface OAuth2TokenProvider {

    /**
     * Request API authorization. This is a foreground action that may produce a dialog to interact with.
     *
     * @param username
     * Username
     * @param activity
     * The responsible activity
     * @param callback
     * A callback to process the asynchronous response
     */
    fun authorizeApi(username: String, activity: Activity, callback: OAuth2TokenProviderAuthCallback)

    /**
     * Fetch a token. No guarantees are provided for validity.
     */
    @Throws(AuthenticationFailedException::class)
    fun getToken(username: String): String

    /**
     * Invalidate the token for this username.
     *
     *
     *
     * Note that the token should always be invalidated on credential failure. However invalidating a token every
     * single time is not recommended.
     *
     *
     * Invalidating a token and then failure with a new token should be treated as a permanent failure.
     */
    fun invalidateToken(username: String)

    /**
     * Get types of supported accounts.
     */
    fun getSupportedAccountTypes(): Array<String>

    /**
     * Provides an asynchronous response to an
     * [OAuth2TokenProvider.authorizeApi] request.
     */
    interface OAuth2TokenProviderAuthCallback {
        fun success(storeServerHost: String, transportServerHost: String)
        fun failure(e: AuthorizationException)
    }
}
