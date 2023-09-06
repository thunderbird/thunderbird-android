package app.k9mail.feature.account.server.validation.domain.usecase

import com.fsck.k9.mail.oauth.AuthStateStorage

class FakeAuthStateStorage(
    private var authorizationState: String? = null,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? = authorizationState

    override fun updateAuthorizationState(authorizationState: String?) {
        this.authorizationState = authorizationState
    }
}
