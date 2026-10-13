package app.k9mail.feature.account.oauth.domain

import java.net.URI

/** Returns whether [redirectUri] matches one of the OAuth callback filters registered by the app. */
fun isRegisteredOAuthRedirectUri(redirectUri: String, applicationId: String): Boolean =
    applicationId.isNotBlank() &&
        runCatching { URI(redirectUri.trim()) }.getOrNull()
            ?.isRegisteredFor(applicationId) == true

private fun URI.isRegisteredFor(applicationId: String): Boolean =
    isAbsolute && hasSupportedComponents() && usesRegisteredScheme(applicationId)

private fun URI.hasSupportedComponents(): Boolean =
    rawUserInfo == null && rawQuery == null && rawFragment == null

private fun URI.usesRegisteredScheme(applicationId: String): Boolean = when (scheme) {
    // RedirectUriReceiverActivity registers this scheme without a host/path restriction.
    applicationId -> true

    "msauth" -> host.equals(applicationId, ignoreCase = true)

    else -> false
}
