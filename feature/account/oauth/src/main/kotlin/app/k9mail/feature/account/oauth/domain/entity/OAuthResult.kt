package app.k9mail.feature.account.oauth.domain.entity

import app.k9mail.feature.account.common.domain.entity.AuthorizationState

sealed interface OAuthResult {
    data class Success(
        val authorizationState: AuthorizationState,
    ) : OAuthResult

    object Failure : OAuthResult
}
