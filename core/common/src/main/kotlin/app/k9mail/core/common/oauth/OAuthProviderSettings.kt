package app.k9mail.core.common.oauth

data class OAuthProviderSettings(
    val applicationId: String,
    val clientIds: Map<OAuthProvider, String>,
    val redirectUriIds: Map<OAuthProvider, String>,
) {
    init {
        require(applicationId.isNotBlank()) {
            "Application id must be set"
        }

        require(clientIds.isNotEmpty()) {
            "Client ids must be set"
        }

        for (provider in OAuthProvider.values()) {
            require(clientIds[provider].isNullOrBlank().not()) {
                "Client id for $provider must be set"
            }
        }

        require(redirectUriIds.isNotEmpty()) {
            "Redirect URI ids must be set"
        }

        require(redirectUriIds[OAuthProvider.MICROSOFT].isNullOrBlank().not()) {
            "Microsoft redirect URI id must be set"
        }
    }
}
