package net.thunderbird.core.common.mail

/** Account-specific Zoho Mail REST configuration stored with that account's incoming server settings. */
object ZohoMailServerSettings {
    const val EXTRA_CLIENT_ID = "zohoOAuthClientId"
    const val EXTRA_REDIRECT_URI = "zohoOAuthRedirectUri"
    const val EXTRA_INCOMING_ONLY = "zohoIncomingOnly"
    const val EXTRA_API_ORIGIN = "zohoMailApiOrigin"

    const val AUTHORIZATION_ENDPOINT = "https://accounts.zoho.com/oauth/v2/auth"
    const val TOKEN_ENDPOINT = "https://accounts.zoho.com/oauth/v2/token"
    const val REDIRECT_PATH = "//oauth2redirect"
    const val LEGACY_REDIRECT_PATH = "/oauth2redirect"
    const val SCOPE_SEPARATOR = ","

    val OAUTH_SCOPES = listOf(
        "ZohoMail.accounts.READ",
        "ZohoMail.folders.READ",
        "ZohoMail.messages.READ",
        "ZohoMail.messages.CREATE",
        "ZohoMail.messages.UPDATE",
        "ZohoMail.messages.DELETE",
        "ZohoMail.folders.UPDATE",
    )
}
