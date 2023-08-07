package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState

class AuthorizationStateRepository : DomainContract.AuthorizationStateRepository {
    override fun isAuthorized(authorizationState: AuthorizationState): Boolean {
        val authState = authorizationState.toAuthState()

        return authState.isAuthorized
    }
}
