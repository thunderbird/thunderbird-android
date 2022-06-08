package com.fsck.k9.auth

import com.fsck.k9.BuildConfig
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider

fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
    val redirectUriSlash = BuildConfig.APPLICATION_ID + ":/oauth2redirect"
    val redirectUriDoubleSlash = BuildConfig.APPLICATION_ID + "://oauth2redirect"

    val googleConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_GMAIL_CLIENT_ID,
        scopes = listOf("https://mail.google.com/"),
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        redirectUri = redirectUriSlash
    )

    return OAuthConfigurationProvider(
        configurations = mapOf(
            listOf("imap.gmail.com", "imap.googlemail.com", "smtp.gmail.com", "smtp.googlemail.com") to googleConfig,
            listOf("imap.mail.yahoo.com", "smtp.mail.yahoo.com") to OAuthConfiguration(
                clientId = BuildConfig.OAUTH_YAHOO_CLIENT_ID,
                scopes = listOf("mail-w"),
                authorizationEndpoint = "https://api.login.yahoo.com/oauth2/request_auth",
                tokenEndpoint = "https://api.login.yahoo.com/oauth2/get_token",
                redirectUri = redirectUriDoubleSlash
            ),
            listOf("imap.aol.com", "smtp.aol.com") to OAuthConfiguration(
                clientId = BuildConfig.OAUTH_AOL_CLIENT_ID,
                scopes = listOf("mail-w"),
                authorizationEndpoint = "https://api.login.aol.com/oauth2/request_auth",
                tokenEndpoint = "https://api.login.aol.com/oauth2/get_token",
                redirectUri = redirectUriDoubleSlash
            ),
        ),
        googleConfiguration = googleConfig
    )
}
