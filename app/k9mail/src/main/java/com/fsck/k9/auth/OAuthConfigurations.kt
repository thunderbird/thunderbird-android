package com.fsck.k9.auth

import com.fsck.k9.BuildConfig
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider

fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
    val googleConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_GMAIL_CLIENT_ID,
        scopes = listOf("https://mail.google.com/"),
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        redirectUri = BuildConfig.APPLICATION_ID + ":/oauth2redirect"
    )

    var outlookConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_OUTLOOK_CLIENT_ID,
        scopes = listOf("wl.imap", "wl.emails", "wl.offline_access"),
        authorizationEndpoint = "https://login.live.com/oauth20_authorize.srf",
        tokenEndpoint = "https://login.live.com/oauth20_token.srf",
        redirectUri = BuildConfig.APPLICATION_ID + "://oauth2redirect"
    )

    var officeConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_OUTLOOK_CLIENT_ID,
        scopes = listOf("https://outlook.office365.com/IMAP.AccessAsUser.All", "https://outlook.office365.com/SMTP.Send", "offline_access"),
        authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        redirectUri = BuildConfig.APPLICATION_ID + "://oauth2redirect"
    )

    return OAuthConfigurationProvider(
        configurations = mapOf(
            listOf("imap.gmail.com", "imap.googlemail.com", "smtp.gmail.com", "smtp.googlemail.com") to googleConfig,
            listOf("imap-mail.outlook.com", "smtp-mail.outlook.com") to outlookConfig,
            listOf("outlook.office365.com", "smtp.office365.com") to officeConfig,
        ),
        googleConfiguration = googleConfig
    )
}
