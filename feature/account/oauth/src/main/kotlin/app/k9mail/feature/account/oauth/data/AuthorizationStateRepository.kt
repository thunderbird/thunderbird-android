package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.DomainContract

class AuthorizationStateRepository : DomainContract.AuthorizationStateRepository {
    override fun isAuthorized(authorizationState: AuthorizationState): Boolean {
        val authState = authorizationState.toAuthState()

        return authState.isAuthorized
    }
}
