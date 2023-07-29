package com.fsck.k9.mail.oauth

/**
 * Creates an instance of [OAuth2TokenProvider] that uses a given [AuthStateStorage] to retrieve and store the
 * (implementation-specific) authorization state.
 */
fun interface OAuth2TokenProviderFactory {
    fun create(authStateStorage: AuthStateStorage): OAuth2TokenProvider
}
