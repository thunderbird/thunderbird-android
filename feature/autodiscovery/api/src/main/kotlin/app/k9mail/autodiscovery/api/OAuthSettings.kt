package app.k9mail.autodiscovery.api

data class OAuthSettings (
    val scopes: List<String>,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
)
