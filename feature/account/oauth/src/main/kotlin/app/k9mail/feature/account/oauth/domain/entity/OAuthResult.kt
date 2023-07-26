package app.k9mail.feature.account.oauth.domain.entity

sealed interface OAuthResult {
    data class Success(
        val authorizationState: AuthorizationState,
    ) : OAuthResult

    object Failure : OAuthResult
}
