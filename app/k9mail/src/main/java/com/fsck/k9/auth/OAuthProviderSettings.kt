package com.fsck.k9.auth

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import com.fsck.k9.BuildConfig

fun createOAuthProviderSettings(): OAuthProviderSettings {
    return OAuthProviderSettings(
        applicationId = BuildConfig.APPLICATION_ID,
        clientIds = mapOf(
            OAuthProvider.AOL to BuildConfig.OAUTH_AOL_CLIENT_ID,
            OAuthProvider.GMAIL to BuildConfig.OAUTH_GMAIL_CLIENT_ID,
            OAuthProvider.MICROSOFT to BuildConfig.OAUTH_MICROSOFT_CLIENT_ID,
            OAuthProvider.YAHOO to BuildConfig.OAUTH_YAHOO_CLIENT_ID,
        ),
        redirectUriIds = mapOf(
            OAuthProvider.MICROSOFT to BuildConfig.OAUTH_MICROSOFT_REDIRECT_URI_ID,
        ),
    )
}
