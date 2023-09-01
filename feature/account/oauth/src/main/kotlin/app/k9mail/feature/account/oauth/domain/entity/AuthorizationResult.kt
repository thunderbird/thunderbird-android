package app.k9mail.feature.account.oauth.domain.entity

import app.k9mail.feature.account.common.domain.entity.AuthorizationState

sealed interface AuthorizationResult {

    data class Success(
        val state: AuthorizationState,
    ) : AuthorizationResult

    data class Failure(
        val error: Exception,
    ) : AuthorizationResult

    object BrowserNotAvailable : AuthorizationResult

    object Canceled : AuthorizationResult
}
