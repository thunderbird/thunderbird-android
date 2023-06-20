package app.k9mail.core.common.oauth

data class OAuthConfiguration(
    val provider: OAuthProvider,
    val clientId: String,
    val scopes: List<String>,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val redirectUri: String,
)
