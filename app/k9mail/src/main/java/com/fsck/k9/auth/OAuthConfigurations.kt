package com.fsck.k9.auth

import com.fsck.k9.BuildConfig
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider

fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
    val googleConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_GMAIL_CLIENT_ID,
        scopes = listOf("https://mail.google.com/"),
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token"
    )

    return OAuthConfigurationProvider(
        configurations = mapOf(
            listOf("imap.gmail.com", "imap.googlemail.com", "smtp.gmail.com", "smtp.googlemail.com") to googleConfig,
        ),
        googleConfiguration = googleConfig
    )
}
