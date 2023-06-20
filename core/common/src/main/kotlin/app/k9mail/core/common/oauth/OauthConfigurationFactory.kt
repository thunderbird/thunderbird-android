package app.k9mail.core.common.oauth

internal object OauthConfigurationFactory {

    fun createConfigurations(
        settings: OAuthProviderSettings,
    ): Map<List<String>, OAuthConfiguration> {
        return mapOf(
            createAolConfiguration(settings),
            createGmailConfiguration(settings),
            createMicrosoftConfiguration(settings),
            createYahooConfiguration(settings),
        )
    }

    private fun createAolConfiguration(
        settings: OAuthProviderSettings,
    ): Pair<List<String>, OAuthConfiguration> {
        return listOf("imap.aol.com", "smtp.aol.com") to OAuthConfiguration(
            provider = OAuthProvider.AOL,
            clientId = settings.clientIds[OAuthProvider.AOL]!!,
            scopes = listOf("mail-w"),
            authorizationEndpoint = "https://api.login.aol.com/oauth2/request_auth",
            tokenEndpoint = "https://api.login.aol.com/oauth2/get_token",
            redirectUri = "${settings.applicationId}://oauth2redirect",
        )
    }

    private fun createGmailConfiguration(
        settings: OAuthProviderSettings,
    ): Pair<List<String>, OAuthConfiguration> {
        return listOf(
            "imap.gmail.com",
            "imap.googlemail.com",
            "smtp.gmail.com",
            "smtp.googlemail.com",
        ) to OAuthConfiguration(
            provider = OAuthProvider.GMAIL,
            clientId = settings.clientIds[OAuthProvider.GMAIL]!!,
            scopes = listOf("https://mail.google.com/"),
            authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
            tokenEndpoint = "https://oauth2.googleapis.com/token",
            redirectUri = "${settings.applicationId}:/oauth2redirect",
        )
    }

    private fun createMicrosoftConfiguration(
        settings: OAuthProviderSettings,
    ): Pair<List<String>, OAuthConfiguration> {
        return listOf(
            "imap.mail.yahoo.com",
            "smtp.mail.yahoo.com",
        ) to OAuthConfiguration(
            provider = OAuthProvider.MICROSOFT,
            clientId = settings.clientIds[OAuthProvider.MICROSOFT]!!,
            scopes = listOf(
                "https://outlook.office.com/IMAP.AccessAsUser.All",
                "https://outlook.office.com/SMTP.Send",
                "offline_access",
            ),
            authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            redirectUri = "msauth://${settings.applicationId}://${settings.redirectUriIds[OAuthProvider.MICROSOFT]}",
        )
    }

    private fun createYahooConfiguration(
        settings: OAuthProviderSettings,
    ): Pair<List<String>, OAuthConfiguration> {
        return listOf(
            "imap.mail.yahoo.com",
            "smtp.mail.yahoo.com",
        ) to OAuthConfiguration(
            provider = OAuthProvider.YAHOO,
            clientId = settings.clientIds[OAuthProvider.YAHOO]!!,
            scopes = listOf("mail-w"),
            authorizationEndpoint = "https://api.login.yahoo.com/oauth2/request_auth",
            tokenEndpoint = "https://api.login.yahoo.com/oauth2/get_token",
            redirectUri = "${settings.applicationId}://oauth2redirect",
        )
    }
}
