package net.thunderbird.android.auth

import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.android.BuildConfig

@Suppress("ktlint:standard:max-line-length")
class TbOAuthConfigurationFactory : OAuthConfigurationFactory {
    override fun createConfigurations(): Map<List<String>, OAuthConfiguration> {
        return mapOf(
            createAolConfiguration(),
            createYahooConfiguration(),
        )
    }

    private fun createAolConfiguration(): Pair<List<String>, OAuthConfiguration> {
        return listOf(
            "imap.aol.com",
            "smtp.aol.com",
        ) to OAuthConfiguration(
            clientId = "dj0yJmk9Rk11R2pTTlF0TlZGJmQ9WVdrOVYxcElTMHBwWVZBbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWVm",
            scopes = listOf("mail-w"),
            authorizationEndpoint = "https://api.login.aol.com/oauth2/request_auth",
            tokenEndpoint = "https://api.login.aol.com/oauth2/get_token",
            redirectUri = "${BuildConfig.APPLICATION_ID}://oauth2redirect",
        )
    }

    private fun createYahooConfiguration(): Pair<List<String>, OAuthConfiguration> {
        return listOf(
            "imap.mail.yahoo.com",
            "smtp.mail.yahoo.com",
        ) to OAuthConfiguration(
            clientId = "dj0yJmk9TTQ2U1JNTlBPQXdJJmQ9WVdrOWVIaHdkMFU0V0hZbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTBi",
            scopes = listOf("mail-w"),
            authorizationEndpoint = "https://api.login.yahoo.com/oauth2/request_auth",
            tokenEndpoint = "https://api.login.yahoo.com/oauth2/get_token",
            redirectUri = "${BuildConfig.APPLICATION_ID}://oauth2redirect",
        )
    }
}