package com.fsck.k9.mail.oauth

interface AuthStateStorage {
    fun getAuthorizationState(): String?
    fun updateAuthorizationState(authorizationState: String?)
}
