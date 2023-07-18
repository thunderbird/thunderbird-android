package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState

class CheckIsAuthorized(
    private val repository: DomainContract.AuthorizationStateRepository,
) : UseCase.CheckIsAuthorized {
    override suspend fun execute(authorizationState: AuthorizationState): Boolean {
        return repository.isAuthorized(authorizationState)
    }
}
