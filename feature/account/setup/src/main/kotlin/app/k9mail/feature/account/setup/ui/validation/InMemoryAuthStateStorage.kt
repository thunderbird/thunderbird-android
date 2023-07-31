package app.k9mail.feature.account.setup.ui.validation

import com.fsck.k9.mail.oauth.AuthStateStorage

class InMemoryAuthStateStorage : AuthStateStorage {
    private var authorizationState: String? = null

    @Synchronized
    override fun getAuthorizationState(): String? {
        return authorizationState
    }

    @Synchronized
    override fun updateAuthorizationState(authorizationState: String?) {
        this.authorizationState = authorizationState
    }
}
