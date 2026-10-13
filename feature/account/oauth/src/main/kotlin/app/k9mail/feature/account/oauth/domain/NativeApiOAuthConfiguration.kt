package app.k9mail.feature.account.oauth.domain

import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfiguration

/** Provider endpoints and least-change scopes used by the native mail API connectors. */
object NativeApiOAuthConfiguration {
    fun defaultFor(protocolType: String, applicationId: String): OAuthConfiguration? {
        if (applicationId.isBlank()) return null

        return when {
            protocolType == Protocols.GMAIL_API -> OAuthConfiguration(
                clientId = "",
                scopes = listOf("openid", "profile", "https://mail.google.com/"),
                authorizationEndpoint = GOOGLE_AUTHORIZATION_ENDPOINT,
                tokenEndpoint = GOOGLE_TOKEN_ENDPOINT,
                redirectUri = "$applicationId:/oauth2redirect",
            )

            Protocols.isGraphProtocol(protocolType) -> OAuthConfiguration(
                clientId = "",
                scopes = listOf("openid", "profile", "email", "offline_access", "Mail.ReadWrite", "Mail.Send"),
                authorizationEndpoint = MICROSOFT_AUTHORIZATION_ENDPOINT,
                tokenEndpoint = MICROSOFT_TOKEN_ENDPOINT,
                redirectUri = "$applicationId://oauth2redirect",
            )

            else -> null
        }
    }

    private const val GOOGLE_AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    private const val MICROSOFT_AUTHORIZATION_ENDPOINT =
        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
    private const val MICROSOFT_TOKEN_ENDPOINT =
        "https://login.microsoftonline.com/common/oauth2/v2.0/token"
}
