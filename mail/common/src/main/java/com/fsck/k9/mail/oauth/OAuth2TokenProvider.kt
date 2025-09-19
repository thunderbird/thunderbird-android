package com.fsck.k9.mail.oauth

import com.fsck.k9.mail.AuthenticationFailedException

interface OAuth2TokenProvider {
    companion object {
        /**
         * A default timeout value to use when fetching tokens.
         */
        const val OAUTH2_TIMEOUT: Int = 30000
    }

    /**
     * A set of usernames fetched from the `id_token`.
     *
     * > Some providers, like Microsoft, require this as they need the primary account email to be the username,
     * > not the email the user entered for SMTP authentication.
     *
     * @throws AuthenticationFailedException If no AuthState is available.
     */
    val usernames: Set<String>
        @Throws(AuthenticationFailedException::class)
        get

    /**
     * Fetch a token. No guarantees are provided for validity.
     */
    @Throws(AuthenticationFailedException::class)
    fun getToken(timeoutMillis: Long): String

    /**
     * Invalidate the token for this username.
     *
     * Note that the token should always be invalidated on credential failure. However invalidating a token every
     * single time is not recommended.
     *
     * Invalidating a token and then failure with a new token should be treated as a permanent failure.
     */
    fun invalidateToken()
}
